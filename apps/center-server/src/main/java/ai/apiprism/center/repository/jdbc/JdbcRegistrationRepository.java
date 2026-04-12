package ai.apiprism.center.repository.jdbc;

import ai.apiprism.center.repository.FileSpecStore;
import ai.apiprism.center.repository.RegistrationRepository;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.model.CanonicalServiceSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 H2 + 文件系统的持久化 RegistrationRepository 实现。
 * 元数据存 H2 service_snapshots 表，大内容（snapshot、rawSpec）存文件系统。
 */
@Repository
@Primary
public class JdbcRegistrationRepository implements RegistrationRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcRegistrationRepository.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final FileSpecStore fileSpecStore;

    public JdbcRegistrationRepository(JdbcTemplate jdbc, ObjectMapper objectMapper, FileSpecStore fileSpecStore) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.fileSpecStore = fileSpecStore;
    }

    @Override
    public StoredRegistration save(StoredRegistration registration) {
        String serviceName = registration.getSnapshot().getRef().getName();
        String environment = registration.getSnapshot().getRef().getEnvironment();

        // 检查 specHash 是否变化，未变化则跳过写入
        Optional<String> existingHash = findSpecHash(serviceName, environment);
        if (existingHash.isPresent() && existingHash.get().equals(registration.getSpecHash())) {
            log.debug("Spec unchanged for {} ({}), skipping save", serviceName, environment);
            return registration;
        }

        // 先写文件
        fileSpecStore.saveSnapshot(registration.getId(), registration.getSnapshot());
        fileSpecStore.saveRawSpec(registration.getId(), registration.getRawSpec(), registration.getSpecFormat());

        try {
            // 再写 DB 元数据
            CanonicalServiceSnapshot snapshot = registration.getSnapshot();
            jdbc.update("""
                    MERGE INTO service_snapshots (id, service_name, environment, title, version,
                        adapter_type, spec_format, spec_hash, warnings, extensions, registered_at)
                    KEY (service_name, environment)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    registration.getId(),
                    serviceName,
                    environment,
                    snapshot.getTitle(),
                    snapshot.getVersion(),
                    registration.getAdapterType(),
                    registration.getSpecFormat(),
                    registration.getSpecHash(),
                    toJson(registration.getWarnings()),
                    toJson(registration.getExtensions()),
                    Timestamp.from(snapshot.getUpdatedAt()));

            log.debug("Saved registration {} for {} ({})", registration.getId(), serviceName, environment);
        } catch (Exception e) {
            // DB 写入失败，清理已写文件
            fileSpecStore.delete(registration.getId(), registration.getSpecFormat());
            throw e;
        }

        return registration;
    }

    @Override
    public Optional<StoredRegistration> findByRef(String serviceName, String environment) {
        List<StoredRegistration> results = jdbc.query(
                "SELECT * FROM service_snapshots WHERE service_name = ? AND environment = ?",
                new RegistrationRowMapper(),
                serviceName, environment);
        return results.stream().findFirst();
    }

    @Override
    public Collection<StoredRegistration> findAll() {
        return jdbc.query("SELECT * FROM service_snapshots ORDER BY service_name, environment",
                new RegistrationRowMapper());
    }

    private Optional<String> findSpecHash(String serviceName, String environment) {
        List<String> hashes = jdbc.query(
                "SELECT spec_hash FROM service_snapshots WHERE service_name = ? AND environment = ?",
                (rs, rowNum) -> rs.getString("spec_hash"),
                serviceName, environment);
        return hashes.stream().findFirst();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize to JSON", e);
        }
    }

    /**
     * 行映射器：从 DB 读取元数据，从文件加载 snapshot 和 rawSpec。
     */
    private class RegistrationRowMapper implements RowMapper<StoredRegistration> {

        @Override
        public StoredRegistration mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString("id");
            String specFormat = rs.getString("spec_format");

            CanonicalServiceSnapshot snapshot = fileSpecStore.loadSnapshot(id);
            String rawSpec = fileSpecStore.loadRawSpec(id, specFormat);

            return StoredRegistration.builder()
                    .id(id)
                    .rawSpec(rawSpec)
                    .specFormat(specFormat)
                    .adapterType(rs.getString("adapter_type"))
                    .specHash(rs.getString("spec_hash"))
                    .snapshot(snapshot)
                    .warnings(fromJsonList(rs.getString("warnings")))
                    .extensions(fromJsonMap(rs.getString("extensions")))
                    .build();
        }

        private List<String> fromJsonList(String json) {
            if (json == null) {
                return List.of();
            }
            try {
                return objectMapper.readValue(json, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize warnings JSON, returning empty list", e);
                return List.of();
            }
        }

        private Map<String, Object> fromJsonMap(String json) {
            if (json == null) {
                return Map.of();
            }
            try {
                return objectMapper.readValue(json, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize extensions JSON, returning empty map", e);
                return Map.of();
            }
        }
    }
}
