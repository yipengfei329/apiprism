package ai.apiprism.center.repository;

import ai.apiprism.center.config.StorageProperties;
import ai.apiprism.model.CanonicalServiceSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件存储服务：将 snapshot JSON 和原始规范落盘到数据目录。
 */
@Component
public class FileSpecStore {

    private static final Logger log = LoggerFactory.getLogger(FileSpecStore.class);

    private final Path snapshotsDir;
    private final Path rawSpecsDir;
    private final ObjectMapper objectMapper;

    public FileSpecStore(StorageProperties properties, ObjectMapper objectMapper) {
        this.snapshotsDir = Path.of(properties.getDataDir(), "snapshots");
        this.rawSpecsDir = Path.of(properties.getDataDir(), "raw-specs");
        this.objectMapper = objectMapper;
    }

    /**
     * 将规范化快照序列化为 JSON 写入 snapshots/{id}.json。
     */
    public void saveSnapshot(String id, CanonicalServiceSnapshot snapshot) {
        Path file = snapshotsDir.resolve(id + ".json");
        try {
            objectMapper.writeValue(file.toFile(), snapshot);
            log.debug("Snapshot saved: {}", file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save snapshot " + id, e);
        }
    }

    /**
     * 从 snapshots/{id}.json 读取并反序列化规范化快照。
     */
    public CanonicalServiceSnapshot loadSnapshot(String id) {
        Path file = snapshotsDir.resolve(id + ".json");
        try {
            return objectMapper.readValue(file.toFile(), CanonicalServiceSnapshot.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load snapshot " + id, e);
        }
    }

    /**
     * 将原始 OpenAPI 规范写入 raw-specs/{id}.{ext}。
     */
    public void saveRawSpec(String id, String content, String specFormat) {
        Path file = rawSpecsDir.resolve(id + "." + resolveExtension(specFormat));
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
            log.debug("Raw spec saved: {}", file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save raw spec " + id, e);
        }
    }

    /**
     * 从 raw-specs/{id}.{ext} 读取原始规范内容。
     */
    public String loadRawSpec(String id, String specFormat) {
        Path file = rawSpecsDir.resolve(id + "." + resolveExtension(specFormat));
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load raw spec " + id, e);
        }
    }

    /**
     * 删除指定 ID 对应的 snapshot 和 raw-spec 文件。
     */
    public void delete(String id, String specFormat) {
        try {
            Files.deleteIfExists(snapshotsDir.resolve(id + ".json"));
            Files.deleteIfExists(rawSpecsDir.resolve(id + "." + resolveExtension(specFormat)));
        } catch (IOException e) {
            log.warn("Failed to clean up files for {}: {}", id, e.getMessage());
        }
    }

    private static String resolveExtension(String specFormat) {
        if (specFormat != null && specFormat.contains("yaml")) {
            return "yaml";
        }
        return "json";
    }
}
