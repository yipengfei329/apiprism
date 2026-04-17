package ai.apiprism.center.registration.persistence;

import ai.apiprism.center.registration.persistence.PersistenceRows.DeploymentRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.EnvironmentRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.ServiceRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.TagRow;
import ai.apiprism.center.registration.persistence.TagRepository.TagSpec;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规范化模型各仓储层的集成单测。使用 @JdbcTest 启动嵌入式 H2 + Flyway，
 * 共享一份 fixture 覆盖常见写入/读取/upsert/diff 场景。
 */
@JdbcTest
@Import({
        ServiceRepository.class,
        EnvironmentRepository.class,
        DeploymentRepository.class,
        TagRepository.class,
        OperationRepository.class,
        OperationChildrenRepository.class,
        OperationVersionRepository.class,
        PersistenceRepositoriesTest.JacksonConfig.class
})
class PersistenceRepositoriesTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class JacksonConfig {
        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired ServiceRepository serviceRepo;
    @Autowired EnvironmentRepository envRepo;
    @Autowired DeploymentRepository deployRepo;
    @Autowired TagRepository tagRepo;
    @Autowired OperationRepository opRepo;
    @Autowired OperationChildrenRepository childrenRepo;
    @Autowired OperationVersionRepository versionRepo;

    private ServiceRow service;
    private EnvironmentRow dev;

    @BeforeEach
    void setup() {
        service = serviceRepo.upsertByName("order-service", "zh-CN");
        dev = envRepo.upsertByCode("dev");
    }

    @Test
    void serviceUpsert_isIdempotent_andPreservesDefaultLocale() {
        ServiceRow again = serviceRepo.upsertByName("order-service", "en-US");
        assertEquals(service.id(), again.id());
        assertEquals("zh-CN", again.defaultLocale(),
                "后续 upsert 不应覆盖首次写入的 default_locale");
    }

    @Test
    void environmentUpsert_isIdempotent() {
        EnvironmentRow again = envRepo.upsertByCode("dev");
        assertEquals(dev.id(), again.id());
    }

    @Test
    void deploymentUpsert_createsAndUpdates() {
        DeploymentRow created = deployRepo.upsert(service.id(), dev.id(), "1.0", "spring-boot",
                "openapi-json", "[\"http://localhost\"]", "hash-1", null, null);
        assertNotNull(created.id());
        assertEquals("hash-1", created.specHash());

        DeploymentRow updated = deployRepo.upsert(service.id(), dev.id(), "1.1", "spring-boot",
                "openapi-json", "[\"http://localhost\"]", "hash-2", null, null);
        assertEquals(created.id(), updated.id());
        assertEquals("hash-2", updated.specHash());
        assertEquals("1.1", updated.version());
    }

    @Test
    void tagUpsert_returnsNameToIdIndex_andUpdatesSlug() {
        Map<String, String> index = tagRepo.upsertAll(service.id(), List.of(
                new TagSpec("User", "user"),
                new TagSpec("Order", "order")));
        assertEquals(2, index.size());
        assertTrue(index.containsKey("User"));

        Map<String, String> again = tagRepo.upsertAll(service.id(), List.of(
                new TagSpec("User", "user-v2")));
        assertEquals(index.get("User"), again.get("User"));

        TagRow user = tagRepo.findByServiceAndName(service.id(), "User").orElseThrow();
        assertEquals("user-v2", user.slug());
    }

    @Test
    void operationInsert_andDefinitionHashIndex() {
        OperationRow op = opRepo.insert(service.id(), "GET /users/{id}", "GET", "/users/{id}", "hash-A", null);
        assertNotNull(op.id());

        Map<String, String> index = opRepo.findDefinitionHashIndex(service.id());
        assertEquals("hash-A", index.get("GET /users/{id}"));

        opRepo.updateStructure(op.id(), "POST", "/users", "hash-B", "[\"apiKey\"]");
        OperationRow reloaded = opRepo.findById(op.id()).orElseThrow();
        assertEquals("POST", reloaded.method());
        assertEquals("hash-B", reloaded.definitionHash());
    }

    @Test
    void operationChildren_replaceCycle() {
        OperationRow op = opRepo.insert(service.id(), "k1", "GET", "/x", "h1", null);

        childrenRepo.insertParameters(op.id(), List.of(
                CanonicalParameter.builder().name("id").location("path").required(true)
                        .schema(Map.of("type", "string")).build()));
        childrenRepo.insertRequestBody(op.id(),
                CanonicalRequestBody.builder().contentType("application/json").required(true)
                        .schema(Map.of("type", "object")).build());
        childrenRepo.insertResponses(op.id(), List.of(
                CanonicalResponse.builder().statusCode("200").contentType("application/json")
                        .schema(Map.of("type", "string")).build()));

        assertEquals(1, childrenRepo.findParameters(op.id()).size());
        assertTrue(childrenRepo.findRequestBody(op.id()).isPresent());
        assertEquals(1, childrenRepo.findResponses(op.id()).size());

        childrenRepo.deleteByOperationId(op.id());
        assertTrue(childrenRepo.findParameters(op.id()).isEmpty());
        assertFalse(childrenRepo.findRequestBody(op.id()).isPresent());
        assertTrue(childrenRepo.findResponses(op.id()).isEmpty());
    }

    @Test
    void deploymentOperations_replaceIsIncremental() {
        DeploymentRow deploy = deployRepo.upsert(service.id(), dev.id(), "1", "a", "f",
                null, "h", null, null);
        OperationRow op1 = opRepo.insert(service.id(), "k1", "GET", "/a", "h1", null);
        OperationRow op2 = opRepo.insert(service.id(), "k2", "GET", "/b", "h2", null);
        OperationRow op3 = opRepo.insert(service.id(), "k3", "GET", "/c", "h3", null);

        deployRepo.replaceDeploymentOperations(deploy.id(), List.of(op1.id(), op2.id()));
        assertEquals(2, deployRepo.findDeploymentOperationIds(deploy.id()).size());

        deployRepo.replaceDeploymentOperations(deploy.id(), List.of(op2.id(), op3.id()));
        List<String> after = deployRepo.findDeploymentOperationIds(deploy.id());
        assertEquals(2, after.size());
        assertTrue(after.contains(op2.id()));
        assertTrue(after.contains(op3.id()));
    }

    @Test
    void tagOperations_replaceIsIncremental() {
        OperationRow op = opRepo.insert(service.id(), "k1", "GET", "/a", "h1", null);
        Map<String, String> tagIds = tagRepo.upsertAll(service.id(), List.of(
                new TagSpec("A", "a"), new TagSpec("B", "b"), new TagSpec("C", "c")));

        tagRepo.replaceOperationTags(op.id(), List.of(tagIds.get("A"), tagIds.get("B")));
        assertEquals(2, tagRepo.findOperationTagIds(op.id()).size());

        tagRepo.replaceOperationTags(op.id(), List.of(tagIds.get("B"), tagIds.get("C")));
        List<String> after = tagRepo.findOperationTagIds(op.id());
        assertEquals(2, after.size());
        assertTrue(after.contains(tagIds.get("B")));
        assertTrue(after.contains(tagIds.get("C")));
    }

    @Test
    void operationVersion_isAppendOnly_andDedupedByHash() {
        OperationRow op = opRepo.insert(service.id(), "k1", "GET", "/a", "h1", null);
        DeploymentRow deploy = deployRepo.upsert(service.id(), dev.id(), "1", "a", "f",
                null, "h", null, null);

        assertTrue(versionRepo.appendIfNew(op.id(), "hash-v1", "{\"v\":1}", deploy.id()));
        assertFalse(versionRepo.appendIfNew(op.id(), "hash-v1", "{\"v\":1}", deploy.id()),
                "相同 hash 不应重复写入");
        assertTrue(versionRepo.appendIfNew(op.id(), "hash-v2", "{\"v\":2}", deploy.id()));

        assertEquals(2, versionRepo.findByOperationId(op.id()).size());
    }

    @Test
    void deploymentTouch_updatesTimestamp() {
        DeploymentRow deploy = deployRepo.upsert(service.id(), dev.id(), "1", "a", "f",
                null, "h", null, null);
        deployRepo.touchLastRegisteredAt(deploy.id());
        DeploymentRow after = deployRepo.findById(deploy.id()).orElseThrow();
        assertTrue(!after.lastRegisteredAt().isBefore(deploy.lastRegisteredAt()));
    }
}
