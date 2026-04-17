package ai.apiprism.center.registration.persistence;

import ai.apiprism.center.registration.persistence.PersistenceRows.OperationRow;
import io.hypersistence.tsid.TSID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * operations 表访问层。接口按 (service_id, operation_key) 唯一。
 * definition_hash 只覆盖结构字段，描述类变化不触发 hash 变化。
 */
@Repository
public class OperationRepository {

    private final JdbcTemplate jdbc;

    public OperationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<OperationRow> findByServiceAndKey(String serviceId, String operationKey) {
        return jdbc.query(SELECT_ALL + " WHERE service_id = ? AND operation_key = ?",
                MAPPER, serviceId, operationKey).stream().findFirst();
    }

    public Optional<OperationRow> findById(String id) {
        return jdbc.query(SELECT_ALL + " WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public List<OperationRow> findByServiceId(String serviceId) {
        return jdbc.query(SELECT_ALL + " WHERE service_id = ? ORDER BY operation_key", MAPPER, serviceId);
    }

    public List<OperationRow> findByIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.query(SELECT_ALL + " WHERE id IN (" + placeholders + ")",
                MAPPER, ids.toArray());
    }

    /**
     * 返回 service 下所有接口的 (operation_key → definition_hash) 索引，供增量 diff。
     */
    public Map<String, String> findDefinitionHashIndex(String serviceId) {
        Map<String, String> result = new HashMap<>();
        jdbc.query("SELECT operation_key, definition_hash FROM operations WHERE service_id = ?",
                rs -> {
                    result.put(rs.getString("operation_key"), rs.getString("definition_hash"));
                }, serviceId);
        return result;
    }

    public OperationRow insert(String serviceId,
                               String operationKey,
                               String method,
                               String path,
                               String definitionHash,
                               String securityRequirementsJson) {
        String id = TSID.Factory.getTsid().toString();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO operations (id, service_id, operation_key, method, path, definition_hash,
                    security_requirements_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, serviceId, operationKey, method, path, definitionHash,
                securityRequirementsJson, Timestamp.from(now), Timestamp.from(now));
        return new OperationRow(id, serviceId, operationKey, method, path, definitionHash,
                securityRequirementsJson, now, now);
    }

    public void updateStructure(String id, String method, String path, String definitionHash,
                                String securityRequirementsJson) {
        jdbc.update("""
                UPDATE operations
                   SET method = ?, path = ?, definition_hash = ?,
                       security_requirements_json = ?, updated_at = ?
                 WHERE id = ?
                """, method, path, definitionHash, securityRequirementsJson,
                Timestamp.from(Instant.now()), id);
    }

    private static final String SELECT_ALL = """
            SELECT id, service_id, operation_key, method, path, definition_hash,
                   security_requirements_json, created_at, updated_at
              FROM operations
            """;

    private static final RowMapper<OperationRow> MAPPER = (rs, i) -> new OperationRow(
            rs.getString("id"),
            rs.getString("service_id"),
            rs.getString("operation_key"),
            rs.getString("method"),
            rs.getString("path"),
            rs.getString("definition_hash"),
            rs.getString("security_requirements_json"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
}
