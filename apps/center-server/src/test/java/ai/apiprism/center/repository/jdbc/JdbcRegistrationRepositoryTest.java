package ai.apiprism.center.repository.jdbc;

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
