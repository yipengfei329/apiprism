package ai.apiprism.center.registration.persistence;

import ai.apiprism.center.registration.persistence.PersistenceRows.OperationDefinitionVersionRow;
import io.hypersistence.tsid.TSID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * operation_definition_versions 的追加日志。
 * 相同 (operation_id, definition_hash) 由唯一约束拦截，保证同一版本不重复写入。
 */
@Repository
public class OperationVersionRepository {

    private final JdbcTemplate jdbc;

    public OperationVersionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 存在相同 (opId, hash) 返回 false，不重复写；否则追加并返回 true。
     */
    public boolean appendIfNew(String operationId,
                               String definitionHash,
                               String definitionJson,
                               String sourceDeploymentId) {
        try {
            jdbc.update("""
                    INSERT INTO operation_definition_versions
                        (id, operation_id, definition_hash, definition_json, source_deployment_id, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    TSID.Factory.getTsid().toString(), operationId, definitionHash,
                    definitionJson, sourceDeploymentId, Timestamp.from(Instant.now()));
            return true;
        } catch (DuplicateKeyException dup) {
            return false;
        }
    }

    public List<OperationDefinitionVersionRow> findByOperationId(String operationId) {
        return jdbc.query("""
                SELECT id, operation_id, definition_hash, definition_json, source_deployment_id, created_at
                  FROM operation_definition_versions
                 WHERE operation_id = ?
                 ORDER BY created_at, id
                """, MAPPER, operationId);
    }

    private static final RowMapper<OperationDefinitionVersionRow> MAPPER = (rs, i) -> new OperationDefinitionVersionRow(
            rs.getString("id"),
            rs.getString("operation_id"),
            rs.getString("definition_hash"),
            rs.getString("definition_json"),
            rs.getString("source_deployment_id"),
            rs.getTimestamp("created_at").toInstant());
}
