package ai.apiprism.center.repository.jdbc;

import ai.apiprism.center.exceptions.RevisionNotFoundException;
import ai.apiprism.center.repository.RevisionSummary;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JdbcRegistrationRepositoryTest {

    @Autowired
    private JdbcRegistrationRepository repository;

    @Test
    void saveAndFindByRef() {
        StoredRegistration reg = buildRegistration("svc-a", "dev", "001");
        repository.save(reg);

        Optional<StoredRegistration> found = repository.findByRef("svc-a", "dev");
        assertTrue(found.isPresent());
        assertEquals("svc-a", found.get().getSnapshot().getRef().getName());
        assertEquals("dev", found.get().getSnapshot().getRef().getEnvironment());
        assertEquals("Test Service A", found.get().getSnapshot().getTitle());
        assertEquals("1.0.0", found.get().getSnapshot().getVersion());
        assertEquals("openapi-json", found.get().getSpecFormat());
        assertEquals("spring-boot-starter", found.get().getAdapterType());
        assertNotNull(found.get().getSpecHash());
        assertNotNull(found.get().getRawSpec());
    }

    @Test
    void findByRefReturnsEmptyForMissing() {
        Optional<StoredRegistration> found = repository.findByRef("nonexistent", "prod");
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllReturnsAllRegistrations() {
        repository.save(buildRegistration("svc-b", "dev", "002"));
        repository.save(buildRegistration("svc-c", "prod", "003"));

        Collection<StoredRegistration> all = repository.findAll();
        assertTrue(all.size() >= 2);
    }

    @Test
    void saveOverwritesExistingRegistration() {
        repository.save(buildRegistration("svc-d", "dev", "004", "1.0.0", "hash-v1"));

        // 不同 hash，应更新
        repository.save(buildRegistration("svc-d", "dev", "005", "2.0.0", "hash-v2"));

        Optional<StoredRegistration> found = repository.findByRef("svc-d", "dev");
        assertTrue(found.isPresent());
        assertEquals("2.0.0", found.get().getSnapshot().getVersion());
    }

    @Test
    void saveSkipsWhenSpecHashUnchanged() {
        repository.save(buildRegistration("svc-e", "dev", "006", "1.0.0", "same-hash"));

        // 相同 hash，应跳过
        StoredRegistration result = repository.save(
                buildRegistration("svc-e", "dev", "007", "2.0.0", "same-hash"));

        Optional<StoredRegistration> found = repository.findByRef("svc-e", "dev");
        assertTrue(found.isPresent());
        // 版本应仍为 1.0.0（跳过了更新）
        assertEquals("1.0.0", found.get().getSnapshot().getVersion());
    }

    @Test
    void saveAndLoadComplexSnapshot() {
        // 测试包含参数、请求体、响应、schema 的复杂快照
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("complex-svc").environment("dev").build())
                .title("Complex Service")
                .version("3.0.0")
                .serverUrl("http://localhost:8080")
                .group(CanonicalGroup.builder()
                        .name("orders")
                        .description("Order management")
                        .operation(CanonicalOperation.builder()
                                .operationId("createOrder")
                                .method("POST")
                                .path("/orders")
                                .summary("Create an order")
                                .description("Creates a new order with items")
                                .tag("orders")
                                .securityRequirement("bearerAuth")
                                .parameter(CanonicalParameter.builder()
                                        .name("X-Request-Id")
                                        .location("header")
                                        .required(false)
                                        .schema(Map.of("type", "string", "format", "uuid"))
                                        .description("Idempotency key")
                                        .build())
                                .requestBody(CanonicalRequestBody.builder()
                                        .required(true)
                                        .contentType("application/json")
                                        .schema(Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "items", Map.of("type", "array"),
                                                        "total", Map.of("type", "number")
                                                )
                                        ))
                                        .build())
                                .response(CanonicalResponse.builder()
                                        .statusCode("201")
                                        .description("Created")
                                        .contentType("application/json")
                                        .schema(Map.of("type", "object"))
                                        .build())
                                .response(CanonicalResponse.builder()
                                        .statusCode("400")
                                        .description("Bad request")
                                        .build())
                                .build())
                        .build())
                .updatedAt(Instant.now())
                .build();

        StoredRegistration reg = StoredRegistration.builder()
                .id("008")
                .rawSpec("{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"Complex\"}}")
                .specFormat("openapi-json")
                .adapterType("spring-boot-starter")
                .specHash("complex-hash-001")
                .snapshot(snapshot)
                .warnings(List.of("Circular reference detected in OrderItem schema"))
                .extensions(Map.of("framework", "spring-boot", "version", "3.3.6"))
                .build();

        repository.save(reg);

        Optional<StoredRegistration> found = repository.findByRef("complex-svc", "dev");
        assertTrue(found.isPresent());

        StoredRegistration loaded = found.get();
        assertEquals(1, loaded.getSnapshot().getGroups().size());
        CanonicalOperation op = loaded.getSnapshot().getGroups().get(0).getOperations().get(0);
        assertEquals("createOrder", op.getOperationId());
        assertEquals("POST", op.getMethod());
        assertEquals(1, op.getParameters().size());
        assertNotNull(op.getRequestBody());
        assertEquals(2, op.getResponses().size());
        assertEquals(1, loaded.getWarnings().size());
        assertEquals("spring-boot", loaded.getExtensions().get("framework"));
    }

    @Test
    void deleteByRefRemovesRegistration() {
        repository.save(buildRegistration("svc-del", "staging", "del-001"));
        assertTrue(repository.findByRef("svc-del", "staging").isPresent());

        repository.deleteByRef("svc-del", "staging");

        assertTrue(repository.findByRef("svc-del", "staging").isEmpty());
    }

    @Test
    void deleteByRefIsIdempotentForMissingRef() {
        // 对不存在的 ref 调用不应抛异常
        assertDoesNotThrow(() -> repository.deleteByRef("nonexistent-svc", "prod"));
    }

    @Test
    void saveRevisionFirstTimeCreatesSeq1AndMarksCurrent() {
        StoredRegistration saved = repository.saveRevision(
                buildRegistration("svc-rev1", "dev", "rev1-001", "1.0.0", "hash-rev1-1"));

        assertEquals(1L, saved.getRevisionSeq());
        assertTrue(saved.isCurrent());

        List<RevisionSummary> revisions = repository.listRevisions("svc-rev1", "dev");
        assertEquals(1, revisions.size());
        assertEquals(1L, revisions.get(0).getSeq());
        assertTrue(revisions.get(0).isCurrent());
    }

    @Test
    void saveRevisionSameHashAsCurrentDoesNotAppend() {
        repository.saveRevision(buildRegistration("svc-rev2", "dev", "rev2-001", "1.0.0", "same-hash"));
        // 不同 id，相同 hash —— 应复用 current，不追加
        StoredRegistration reused = repository.saveRevision(
                buildRegistration("svc-rev2", "dev", "rev2-002", "2.0.0", "same-hash"));

        assertEquals("rev2-001", reused.getId(), "current id 应保持不变");
        assertEquals(1, repository.listRevisions("svc-rev2", "dev").size());
    }

    @Test
    void saveRevisionNewHashAppendsAndAdvancesCurrent() {
        repository.saveRevision(buildRegistration("svc-rev3", "dev", "rev3-001", "1.0.0", "hash-rev3-1"));
        StoredRegistration second = repository.saveRevision(
                buildRegistration("svc-rev3", "dev", "rev3-002", "2.0.0", "hash-rev3-2"));

        assertEquals(2L, second.getRevisionSeq());
        assertEquals("rev3-002", repository.findCurrent("svc-rev3", "dev").orElseThrow().getId());

        List<RevisionSummary> revisions = repository.listRevisions("svc-rev3", "dev");
        assertEquals(2, revisions.size());
        assertEquals(2L, revisions.get(0).getSeq(), "按 seq 降序");
        assertTrue(revisions.get(0).isCurrent());
        assertFalse(revisions.get(1).isCurrent());
    }

    @Test
    void activateRevisionSwitchesCurrentAndIsIdempotent() {
        repository.saveRevision(buildRegistration("svc-rev4", "dev", "rev4-001", "1.0.0", "hash-rev4-1"));
        repository.saveRevision(buildRegistration("svc-rev4", "dev", "rev4-002", "2.0.0", "hash-rev4-2"));

        StoredRegistration rolled = repository.activateRevision("svc-rev4", "dev", "rev4-001");
        assertEquals("rev4-001", rolled.getId());
        assertEquals("1.0.0", repository.findCurrent("svc-rev4", "dev").orElseThrow().getSnapshot().getVersion());

        // 幂等：再次 activate 同一 id 不抛异常
        StoredRegistration again = repository.activateRevision("svc-rev4", "dev", "rev4-001");
        assertEquals("rev4-001", again.getId());

        // listRevisions 仍然返回两条，但 current 标志已切换
        List<RevisionSummary> revisions = repository.listRevisions("svc-rev4", "dev");
        assertEquals(2, revisions.size());
        assertTrue(revisions.stream().filter(RevisionSummary::isCurrent).findFirst()
                .filter(r -> r.getId().equals("rev4-001")).isPresent());
    }

    @Test
    void activateRevisionUnknownIdThrows() {
        repository.saveRevision(buildRegistration("svc-rev5", "dev", "rev5-001", "1.0.0", "hash-rev5-1"));
        assertThrows(RevisionNotFoundException.class,
                () -> repository.activateRevision("svc-rev5", "dev", "nonexistent"));
    }

    @Test
    void registrationAfterRollbackAppendsNewRevision() {
        repository.saveRevision(buildRegistration("svc-rev6", "dev", "rev6-001", "1.0.0", "hash-rev6-1"));
        repository.saveRevision(buildRegistration("svc-rev6", "dev", "rev6-002", "2.0.0", "hash-rev6-2"));
        repository.activateRevision("svc-rev6", "dev", "rev6-001");

        // 回滚后 adapter 又推了最新 hash —— 应作为新 revision 追加，不是复用历史 id
        StoredRegistration latest = repository.saveRevision(
                buildRegistration("svc-rev6", "dev", "rev6-003", "3.0.0", "hash-rev6-2"));
        assertEquals("rev6-003", latest.getId());
        assertEquals(3L, latest.getRevisionSeq());
        assertEquals(3, repository.listRevisions("svc-rev6", "dev").size());
    }

    @Test
    void deleteByRefRemovesAllRevisions() {
        repository.saveRevision(buildRegistration("svc-del-all", "dev", "del-r1", "1.0.0", "hash-d1"));
        repository.saveRevision(buildRegistration("svc-del-all", "dev", "del-r2", "2.0.0", "hash-d2"));
        assertEquals(2, repository.listRevisions("svc-del-all", "dev").size());

        repository.deleteByRef("svc-del-all", "dev");

        assertTrue(repository.findCurrent("svc-del-all", "dev").isEmpty());
        assertTrue(repository.listRevisions("svc-del-all", "dev").isEmpty());
    }

    @Test
    void deleteByRefDoesNotAffectOtherEnvironments() {
        repository.save(buildRegistration("svc-multi", "dev", "multi-001"));
        repository.save(buildRegistration("svc-multi", "prod", "multi-002", "1.0.0", "hash-multi-002"));

        repository.deleteByRef("svc-multi", "dev");

        assertTrue(repository.findByRef("svc-multi", "dev").isEmpty());
        assertTrue(repository.findByRef("svc-multi", "prod").isPresent());
    }

    private StoredRegistration buildRegistration(String name, String env, String id) {
        return buildRegistration(name, env, id, "1.0.0", "hash-" + id);
    }

    private StoredRegistration buildRegistration(String name, String env, String id,
                                                  String version, String specHash) {
        return StoredRegistration.builder()
                .id(id)
                .rawSpec("{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"" + name + "\"}}")
                .specFormat("openapi-json")
                .adapterType("spring-boot-starter")
                .specHash(specHash)
                .snapshot(CanonicalServiceSnapshot.builder()
                        .ref(ServiceRef.builder().name(name).environment(env).build())
                        .title("Test Service " + name.substring(name.length() - 1).toUpperCase())
                        .version(version)
                        .serverUrl("http://localhost:8080")
                        .group(CanonicalGroup.builder()
                                .name("default")
                                .operation(CanonicalOperation.builder()
                                        .operationId("ping")
                                        .method("GET")
                                        .path("/ping")
                                        .build())
                                .build())
                        .updatedAt(Instant.now())
                        .build())
                .warnings(List.of())
                .extensions(Map.of())
                .build();
    }
}
