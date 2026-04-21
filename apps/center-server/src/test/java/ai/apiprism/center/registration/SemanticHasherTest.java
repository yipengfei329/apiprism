package ai.apiprism.center.registration;

import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SemanticHasherTest {

    @Test
    void sameSnapshotProducesSameHash() {
        CanonicalServiceSnapshot s = buildSnapshot("1.0.0");
        assertEquals(SemanticHasher.hash(s), SemanticHasher.hash(s));
    }

    @Test
    void updatedAtDifferenceDoesNotChangeHash() {
        // updatedAt 不参与 hash，两个时间戳不同的 snapshot 应产生相同 hash
        CanonicalServiceSnapshot s1 = buildSnapshot("1.0.0").toBuilder()
                .updatedAt(Instant.ofEpochMilli(1000)).build();
        CanonicalServiceSnapshot s2 = buildSnapshot("1.0.0").toBuilder()
                .updatedAt(Instant.ofEpochMilli(9999)).build();
        assertEquals(SemanticHasher.hash(s1), SemanticHasher.hash(s2));
    }

    @Test
    void differentVersionChangesHash() {
        CanonicalServiceSnapshot s1 = buildSnapshot("1.0.0");
        CanonicalServiceSnapshot s2 = buildSnapshot("2.0.0");
        assertNotEquals(SemanticHasher.hash(s1), SemanticHasher.hash(s2));
    }

    @Test
    void addingEndpointChangesHash() {
        CanonicalServiceSnapshot s1 = buildSnapshot("1.0.0");
        CanonicalServiceSnapshot s2 = s1.toBuilder()
                .groups(List.of(CanonicalGroup.builder()
                        .name("default")
                        .slug("default")
                        .operation(CanonicalOperation.builder()
                                .operationId("getUser")
                                .method("GET")
                                .path("/users/{id}")
                                .build())
                        .operation(CanonicalOperation.builder()
                                .operationId("createUser")
                                .method("POST")
                                .path("/users")
                                .build())
                        .build()))
                .build();
        assertNotEquals(SemanticHasher.hash(s1), SemanticHasher.hash(s2));
    }

    @Test
    void operationSortOrderIsStable() {
        // 两组 operation 顺序不同但内容相同，因在同一 group 内按 method+path 排序，hash 应相同
        CanonicalOperation get = CanonicalOperation.builder()
                .operationId("getUser").method("GET").path("/users/{id}").build();
        CanonicalOperation post = CanonicalOperation.builder()
                .operationId("createUser").method("POST").path("/users").build();

        CanonicalServiceSnapshot s1 = snapshotWithOps(List.of(get, post));
        CanonicalServiceSnapshot s2 = snapshotWithOps(List.of(post, get));
        assertEquals(SemanticHasher.hash(s1), SemanticHasher.hash(s2));
    }

    @Test
    void schemaMapKeyOrderDoesNotChangeHash() {
        // schema 中 key 顺序不同（TreeMap 会标准化），hash 应相同
        CanonicalServiceSnapshot s1 = snapshotWithSchema(Map.of("type", "string", "format", "uuid"));
        CanonicalServiceSnapshot s2 = snapshotWithSchema(Map.of("format", "uuid", "type", "string"));
        assertEquals(SemanticHasher.hash(s1), SemanticHasher.hash(s2));
    }

    @Test
    void changingParameterSchemaChangesHash() {
        CanonicalServiceSnapshot s1 = snapshotWithSchema(Map.of("type", "string"));
        CanonicalServiceSnapshot s2 = snapshotWithSchema(Map.of("type", "integer"));
        assertNotEquals(SemanticHasher.hash(s1), SemanticHasher.hash(s2));
    }

    @Test
    void hashHasExpectedLength() {
        // SHA-256 hex = 64 chars
        String h = SemanticHasher.hash(buildSnapshot("1.0.0"));
        assertNotNull(h);
        assertEquals(64, h.length());
    }

    @Test
    void versionPrefixIsSmh1() {
        assertEquals("smh:1", SemanticHasher.VERSION);
    }

    // ---- 辅助方法 ----

    private CanonicalServiceSnapshot buildSnapshot(String version) {
        return CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("test-svc").environment("dev").build())
                .title("Test Service")
                .version(version)
                .serverUrl("http://localhost:8080")
                .group(CanonicalGroup.builder()
                        .name("default")
                        .slug("default")
                        .operation(CanonicalOperation.builder()
                                .operationId("getUser")
                                .method("GET")
                                .path("/users/{id}")
                                .parameter(CanonicalParameter.builder()
                                        .name("id")
                                        .location("path")
                                        .required(true)
                                        .schema(Map.of("type", "string"))
                                        .build())
                                .response(CanonicalResponse.builder()
                                        .statusCode("200")
                                        .contentType("application/json")
                                        .schema(Map.of("type", "object"))
                                        .build())
                                .build())
                        .build())
                .updatedAt(Instant.now())
                .build();
    }

    private CanonicalServiceSnapshot snapshotWithOps(List<CanonicalOperation> ops) {
        return CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("test-svc").environment("dev").build())
                .title("Test Service")
                .version("1.0.0")
                .group(CanonicalGroup.builder()
                        .name("default")
                        .slug("default")
                        .operations(ops)
                        .build())
                .updatedAt(Instant.now())
                .build();
    }

    private CanonicalServiceSnapshot snapshotWithSchema(Map<String, Object> schema) {
        return CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("test-svc").environment("dev").build())
                .title("Test Service")
                .version("1.0.0")
                .group(CanonicalGroup.builder()
                        .name("default")
                        .slug("default")
                        .operation(CanonicalOperation.builder()
                                .operationId("doSomething")
                                .method("POST")
                                .path("/items")
                                .requestBody(CanonicalRequestBody.builder()
                                        .required(true)
                                        .contentType("application/json")
                                        .schema(schema)
                                        .build())
                                .build())
                        .build())
                .updatedAt(Instant.now())
                .build();
    }
}
