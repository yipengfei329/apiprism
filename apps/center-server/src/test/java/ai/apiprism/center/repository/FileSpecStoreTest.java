package ai.apiprism.center.repository;

import ai.apiprism.center.config.StorageProperties;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FileSpecStoreTest {

    @TempDir
    Path tempDir;

    private FileSpecStore store;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(tempDir.resolve("snapshots"));
        Files.createDirectories(tempDir.resolve("raw-specs"));

        StorageProperties props = new StorageProperties();
        props.setDataDir(tempDir.toString());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        store = new FileSpecStore(props, mapper);
    }

    @Test
    void saveAndLoadSnapshot() {
        CanonicalServiceSnapshot snapshot = buildSnapshot();

        store.saveSnapshot("test-001", snapshot);
        assertTrue(Files.exists(tempDir.resolve("snapshots/test-001.json")));

        CanonicalServiceSnapshot loaded = store.loadSnapshot("test-001");
        assertEquals(snapshot.getRef().getName(), loaded.getRef().getName());
        assertEquals(snapshot.getRef().getEnvironment(), loaded.getRef().getEnvironment());
        assertEquals(snapshot.getTitle(), loaded.getTitle());
        assertEquals(snapshot.getVersion(), loaded.getVersion());
        assertEquals(snapshot.getGroups().size(), loaded.getGroups().size());
        assertEquals(snapshot.getGroups().get(0).getOperations().size(),
                loaded.getGroups().get(0).getOperations().size());
    }

    @Test
    void saveAndLoadRawSpecJson() {
        String content = "{\"openapi\":\"3.0.0\"}";

        store.saveRawSpec("test-002", content, "openapi-json");
        assertTrue(Files.exists(tempDir.resolve("raw-specs/test-002.json")));

        String loaded = store.loadRawSpec("test-002", "openapi-json");
        assertEquals(content, loaded);
    }

    @Test
    void saveAndLoadRawSpecYaml() {
        String content = "openapi: '3.0.0'";

        store.saveRawSpec("test-003", content, "openapi-yaml");
        assertTrue(Files.exists(tempDir.resolve("raw-specs/test-003.yaml")));

        String loaded = store.loadRawSpec("test-003", "openapi-yaml");
        assertEquals(content, loaded);
    }

    @Test
    void deleteRemovesFiles() {
        store.saveSnapshot("test-004", buildSnapshot());
        store.saveRawSpec("test-004", "{}", "openapi-json");

        store.delete("test-004", "openapi-json");
        assertFalse(Files.exists(tempDir.resolve("snapshots/test-004.json")));
        assertFalse(Files.exists(tempDir.resolve("raw-specs/test-004.json")));
    }

    private CanonicalServiceSnapshot buildSnapshot() {
        return CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("demo-service").environment("dev").build())
                .title("Demo Service")
                .version("1.0.0")
                .serverUrl("http://localhost:8080")
                .group(CanonicalGroup.builder()
                        .name("orders")
                        .description("Order operations")
                        .operation(CanonicalOperation.builder()
                                .operationId("getOrder")
                                .method("GET")
                                .path("/orders/{id}")
                                .summary("Get order by ID")
                                .build())
                        .build())
                .updatedAt(Instant.now())
                .build();
    }
}
