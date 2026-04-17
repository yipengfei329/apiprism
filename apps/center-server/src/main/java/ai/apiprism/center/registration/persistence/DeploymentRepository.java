package ai.apiprism.center.registration.persistence;

import ai.apiprism.center.registration.persistence.PersistenceRows.DeploymentRow;
import io.hypersistence.tsid.TSID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * service_deployments + deployment_operations 访问层。
 * 一个 (service, environment) 对应一行 deployment；deployment_operations 记录暴露的接口集合。
 */
@Repository
public class DeploymentRepository {

    private final JdbcTemplate jdbc;

    public DeploymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DeploymentRow> findByServiceAndEnv(String serviceId, String environmentId) {
        return jdbc.query(SELECT_ALL + " WHERE service_id = ? AND environment_id = ?",
                MAPPER, serviceId, environmentId).stream().findFirst();
    }

    public Optional<DeploymentRow> findById(String id) {
        return jdbc.query(SELECT_ALL + " WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public List<DeploymentRow> findAll() {
        return jdbc.query(SELECT_ALL + " ORDER BY service_id, environment_id", MAPPER);
    }

    public DeploymentRow upsert(String serviceId,
                                String environmentId,
                                String version,
                                String adapterType,
                                String specFormat,
                                String serverUrlsJson,
                                String specHash,
                                String warningsJson,
                                String extensionsJson) {
        Instant now = Instant.now();
        Optional<DeploymentRow> existing = findByServiceAndEnv(serviceId, environmentId);
        if (existing.isPresent()) {
            jdbc.update("""
                    UPDATE service_deployments
                       SET version = ?, adapter_type = ?, spec_format = ?,
                           server_urls_json = ?, spec_hash = ?,
                           warnings_json = ?, extensions_json = ?, last_registered_at = ?
                     WHERE id = ?
                    """,
                    version, adapterType, specFormat, serverUrlsJson, specHash,
                    warningsJson, extensionsJson, Timestamp.from(now), existing.get().id());
            return new DeploymentRow(existing.get().id(), serviceId, environmentId, version,
                    adapterType, specFormat, serverUrlsJson, specHash, warningsJson, extensionsJson, now);
        }
        String id = TSID.Factory.getTsid().toString();
        jdbc.update("""
                INSERT INTO service_deployments (id, service_id, environment_id, version, adapter_type,
                    spec_format, server_urls_json, spec_hash, warnings_json, extensions_json, last_registered_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, serviceId, environmentId, version, adapterType, specFormat,
                serverUrlsJson, specHash, warningsJson, extensionsJson, Timestamp.from(now));
        return new DeploymentRow(id, serviceId, environmentId, version, adapterType, specFormat,
                serverUrlsJson, specHash, warningsJson, extensionsJson, now);
    }

    /**
     * spec_hash 未变时仅刷新 last_registered_at，避免写放大。
     */
    public void touchLastRegisteredAt(String deploymentId) {
        jdbc.update("UPDATE service_deployments SET last_registered_at = ? WHERE id = ?",
                Timestamp.from(Instant.now()), deploymentId);
    }

    public List<String> findDeploymentOperationIds(String deploymentId) {
        return jdbc.queryForList(
                "SELECT operation_id FROM deployment_operations WHERE deployment_id = ?",
                String.class, deploymentId);
    }

    /**
     * 把 deployment_operations 替换为 opIds 指定的集合。先算差集，仅动需要变的行。
     */
    public void replaceDeploymentOperations(String deploymentId, Collection<String> opIds) {
        List<String> current = findDeploymentOperationIds(deploymentId);
        java.util.Set<String> target = new java.util.HashSet<>(opIds);
        java.util.Set<String> existing = new java.util.HashSet<>(current);

        for (String gone : existing) {
            if (!target.contains(gone)) {
                jdbc.update("DELETE FROM deployment_operations WHERE deployment_id = ? AND operation_id = ?",
                        deploymentId, gone);
            }
        }
        for (String added : target) {
            if (!existing.contains(added)) {
                jdbc.update("INSERT INTO deployment_operations (deployment_id, operation_id) VALUES (?, ?)",
                        deploymentId, added);
            }
        }
    }

    private static final String SELECT_ALL = """
            SELECT id, service_id, environment_id, version, adapter_type, spec_format,
                   server_urls_json, spec_hash, warnings_json, extensions_json, last_registered_at
              FROM service_deployments
            """;

    private static final RowMapper<DeploymentRow> MAPPER = (rs, i) -> new DeploymentRow(
            rs.getString("id"),
            rs.getString("service_id"),
            rs.getString("environment_id"),
            rs.getString("version"),
            rs.getString("adapter_type"),
            rs.getString("spec_format"),
            rs.getString("server_urls_json"),
            rs.getString("spec_hash"),
            rs.getString("warnings_json"),
            rs.getString("extensions_json"),
            rs.getTimestamp("last_registered_at").toInstant());
}
