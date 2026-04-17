package ai.apiprism.center.repository;

import ai.apiprism.center.config.StorageProperties;
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
 * 原始 OpenAPI 规范的审计归档。
 *
 * <p>中心规范化模型切换后，snapshot 结构存在 H2 规范化表里，不再落盘。
 * 本组件只承担一件事：把适配器每次提交的 raw spec 原文按 deployment id 归档，
 * 便于事后比对/审计/调试。查询路径（CatalogService、Markdown 渲染等）不再依赖本组件。
 */
@Component
public class FileSpecStore {

    private static final Logger log = LoggerFactory.getLogger(FileSpecStore.class);

    private final Path rawSpecsDir;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    public FileSpecStore(StorageProperties properties, ObjectMapper objectMapper) {
        this.rawSpecsDir = Path.of(properties.getDataDir(), "raw-specs");
        this.objectMapper = objectMapper;
    }

    /**
     * 将原始 OpenAPI 规范写入 raw-specs/{id}.{ext}，供审计回溯。
     */
    public void saveRawSpec(String id, String content, String specFormat) {
        Path file = rawSpecsDir.resolve(id + "." + resolveExtension(specFormat));
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
            log.debug("Raw spec archived: {}", file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to archive raw spec " + id, e);
        }
    }

    /**
     * 从 raw-specs/{id}.{ext} 读取原始规范，用于审计/调试。
     */
    public String loadRawSpec(String id, String specFormat) {
        Path file = rawSpecsDir.resolve(id + "." + resolveExtension(specFormat));
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load raw spec " + id, e);
        }
    }

    private static String resolveExtension(String specFormat) {
        if (specFormat != null && specFormat.contains("yaml")) {
            return "yaml";
        }
        return "json";
    }
}
