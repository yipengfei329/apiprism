package ai.apiprism.center.repository;

import ai.apiprism.center.config.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FileSpecStore 降级为 raw spec 审计归档后，仅保留写入/读取 raw spec 的验证。
 */
class FileSpecStoreTest {

    @TempDir
    Path tempDir;

    private FileSpecStore store;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(tempDir.resolve("raw-specs"));

        StorageProperties props = new StorageProperties();
        props.setDataDir(tempDir.toString());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        store = new FileSpecStore(props, mapper);
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
}
