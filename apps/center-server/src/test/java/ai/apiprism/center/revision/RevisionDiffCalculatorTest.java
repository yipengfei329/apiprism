package ai.apiprism.center.revision;

import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevisionDiffCalculatorTest {

    @Test
    void firstRevisionAllEndpointsAreAdded() {
        CanonicalServiceSnapshot snapshot = buildSnapshot(
                op("GET", "/users", "listUsers"),
                op("POST", "/users", "createUser"));

        RevisionDiff diff = RevisionDiffCalculator.compute(null, snapshot);

        assertEquals(2, diff.totalEndpoints());
        assertEquals(2, diff.added().size());
        assertTrue(diff.removed().isEmpty());
        assertTrue(diff.modified().isEmpty());
    }

    @Test
    void newEndpointDetectedAsAdded() {
        CanonicalServiceSnapshot prev = buildSnapshot(op("GET", "/users", "listUsers"));
        CanonicalServiceSnapshot curr = buildSnapshot(
                op("GET", "/users", "listUsers"),
                op("POST", "/users", "createUser"));

        RevisionDiff diff = RevisionDiffCalculator.compute(prev, curr);

        assertEquals(2, diff.totalEndpoints());
        assertEquals(1, diff.added().size());
        assertEquals("POST", diff.added().get(0).method());
        assertEquals("/users", diff.added().get(0).path());
        assertTrue(diff.removed().isEmpty());
        assertTrue(diff.modified().isEmpty());
    }

    @Test
    void removedEndpointDetected() {
        CanonicalServiceSnapshot prev = buildSnapshot(
                op("GET", "/users", "listUsers"),
                op("DELETE", "/users/{id}", "deleteUser"));
        CanonicalServiceSnapshot curr = buildSnapshot(op("GET", "/users", "listUsers"));

        RevisionDiff diff = RevisionDiffCalculator.compute(prev, curr);

        assertEquals(1, diff.totalEndpoints());
        assertTrue(diff.added().isEmpty());
        assertEquals(1, diff.removed().size());
        assertEquals("DELETE", diff.removed().get(0).method());
        assertTrue(diff.modified().isEmpty());
    }

    @Test
    void modifiedEndpointDetectedWhenParameterChanges() {
        CanonicalOperation before = CanonicalOperation.builder()
                .operationId("getUser")
                .method("GET")
                .path("/users/{id}")
                .parameter(CanonicalParameter.builder()
                        .name("id")
                        .location("path")
                        .required(true)
                        .schema(Map.of("type", "string"))
                        .build())
                .build();

        CanonicalOperation after = CanonicalOperation.builder()
                .operationId("getUser")
                .method("GET")
                .path("/users/{id}")
                .parameter(CanonicalParameter.builder()
                        .name("id")
                        .location("path")
                        .required(true)
                        .schema(Map.of("type", "integer"))  // 类型从 string 改为 integer
                        .build())
                .build();

        RevisionDiff diff = RevisionDiffCalculator.compute(buildSnapshot(before), buildSnapshot(after));

        assertEquals(1, diff.totalEndpoints());
        assertTrue(diff.added().isEmpty());
        assertTrue(diff.removed().isEmpty());
        assertEquals(1, diff.modified().size());
        assertEquals("GET", diff.modified().get(0).method());
        assertEquals("/users/{id}", diff.modified().get(0).path());
    }

    @Test
    void unchangedEndpointNotInAnyList() {
        CanonicalOperation op = op("GET", "/health", "healthCheck");
        RevisionDiff diff = RevisionDiffCalculator.compute(buildSnapshot(op), buildSnapshot(op));

        assertEquals(1, diff.totalEndpoints());
        assertTrue(diff.added().isEmpty());
        assertTrue(diff.removed().isEmpty());
        assertTrue(diff.modified().isEmpty());
    }

    @Test
    void duplicateMethodPathAcrossGroupsCountedOnce() {
        // 同 method+path 在两个 group 出现时，只取第一次（putIfAbsent）
        CanonicalOperation op1 = op("GET", "/items", "listItems");
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("svc").environment("dev").build())
                .title("T")
                .version("1.0")
                .group(CanonicalGroup.builder().name("g1").slug("g1")
                        .operation(op1).build())
                .group(CanonicalGroup.builder().name("g2").slug("g2")
                        .operation(op1).build())
                .updatedAt(Instant.now())
                .build();

        RevisionDiff diff = RevisionDiffCalculator.compute(null, snapshot);
        assertEquals(1, diff.totalEndpoints());
    }

    // ---- 辅助方法 ----

    private CanonicalOperation op(String method, String path, String operationId) {
        return CanonicalOperation.builder()
                .operationId(operationId)
                .method(method)
                .path(path)
                .build();
    }

    private CanonicalServiceSnapshot buildSnapshot(CanonicalOperation... ops) {
        return CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("svc").environment("dev").build())
                .title("Test")
                .version("1.0")
                .group(CanonicalGroup.builder()
                        .name("default")
                        .slug("default")
                        .operations(List.of(ops))
                        .build())
                .updatedAt(Instant.now())
                .build();
    }
}
