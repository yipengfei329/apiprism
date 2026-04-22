package ai.apiprism.center.repository.jdbc;

import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.center.exceptions.RevisionNotFoundException;
import ai.apiprism.center.repository.FileSpecStore;
import ai.apiprism.center.repository.RegistrationRepository;
import ai.apiprism.center.repository.RevisionSummary;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.center.revision.RevisionDiff;
import ai.apiprism.center.revision.RevisionDiffCalculator;
import ai.apiprism.model.CanonicalServiceSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 H2 + 文件系统的持久化 RegistrationRepository 实现。
 * 版本历史落在 service_snapshot_revisions 表，service_snapshots 退化为 current pointer。
 * 大内容（snapshot JSON、rawSpec）按 revision id 存在 FileSpecStore，历史版本天然隔离。
 */
@Repository
@Primary
public class JdbcRegistrationRepository implements RegistrationRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcRegistrationRepository.class);

    private static final String SOURCE_REGISTER = "REGISTER";

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final FileSpecStore fileSpecStore;

    public JdbcRegistrationRepository(JdbcTemplate jdbc, ObjectMapper objectMapper, FileSpecStore fileSpecStore) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.fileSpecStore = fileSpecStore;
    }

    @Override
    @Transactional
    public StoredRegistration saveRevision(StoredRegistration incoming) {
        String serviceName = incoming.getSnapshot().getRef().getName();
        String environment = incoming.getSnapshot().getRef().getEnvironment();

        Optional<StoredRegistration> currentOpt = findCurrent(serviceName, environment);
        Timestamp now = Timestamp.from(Instant.now());

        // case 1: 与 current hash 相同，仅刷新 last_seen_at，不追加历史
        if (currentOpt.isPresent() && currentOpt.get().getSpecHash().equals(incoming.getSpecHash())) {
            jdbc.update("UPDATE service_snapshots SET last_seen_at = ? WHERE service_name = ? AND environment = ?",
                    now, serviceName, environment);
            log.debug("Spec unchanged for {} ({}), refreshed last_seen_at", serviceName, environment);
            return currentOpt.get();
        }

        // case 2: 追加新 revision，current pointer 切到新 id
        String previousId = currentOpt.map(StoredRegistration::getId).orElse(null);
        long seq = nextRevisionSeq(serviceName, environment);

        // 计算与前驱 revision 的接口变更差异
        CanonicalServiceSnapshot previousSnapshot = currentOpt.map(StoredRegistration::getSnapshot).orElse(null);
        RevisionDiff diff = RevisionDiffCalculator.compute(previousSnapshot, incoming.getSnapshot());

        // 写文件在前，DB 失败时清理文件
        fileSpecStore.saveSnapshot(incoming.getId(), incoming.getSnapshot());
        fileSpecStore.saveRawSpec(incoming.getId(), incoming.getRawSpec(), incoming.getSpecFormat());

        try {
            insertRevisionWithRetry(incoming, serviceName, environment, seq, previousId, diff, now);

            CanonicalServiceSnapshot snapshot = incoming.getSnapshot();
            jdbc.update("""
                    MERGE INTO service_snapshots (id, service_name, environment, title, version,
                        adapter_type, spec_format, spec_hash, warnings, extensions, registered_at,
                        current_revision_id, last_seen_at)
                    KEY (service_name, environment)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    incoming.getId(),
                    serviceName,
                    environment,
                    snapshot.getTitle(),
                    snapshot.getVersion(),
                    incoming.getAdapterType(),
                    incoming.getSpecFormat(),
                    incoming.getSpecHash(),
                    toJson(incoming.getWarnings()),
                    toJson(incoming.getExtensions()),
                    Timestamp.from(snapshot.getUpdatedAt()),
                    incoming.getId(),
                    now);

            log.info("Appended revision #{} (id={}) for {} ({})", seq, incoming.getId(), serviceName, environment);
        } catch (Exception e) {
            fileSpecStore.delete(incoming.getId(), incoming.getSpecFormat());
            throw e;
        }

        return incoming.toBuilder()
                .revisionSeq(seq)
                .source(SOURCE_REGISTER)
                .current(true)
                .registeredAt(now.toInstant())
                .endpointCount(diff.totalEndpoints())
                .diffStats(diff)
                .build();
    }

    private void insertRevisionWithRetry(StoredRegistration incoming, String serviceName, String environment,
                                          long seq, String previousId, RevisionDiff diff, Timestamp now) {
        try {
            insertRevisionRow(incoming, serviceName, environment, seq, previousId, diff, now);
        } catch (DuplicateKeyException e) {
            // 并发情况下 seq 冲突，重新取一次
            long retrySeq = nextRevisionSeq(serviceName, environment);
            log.warn("Revision seq conflict for {} ({}), retrying with seq {}", serviceName, environment, retrySeq);
            insertRevisionRow(incoming, serviceName, environment, retrySeq, previousId, diff, now);
        }
    }

    private void insertRevisionRow(StoredRegistration incoming, String serviceName, String environment,
                                    long seq, String previousId, RevisionDiff diff, Timestamp now) {
        CanonicalServiceSnapshot snapshot = incoming.getSnapshot();
        jdbc.update("""
                INSERT INTO service_snapshot_revisions
                    (id, service_name, environment, revision_seq, spec_hash, title, version,
                     adapter_type, spec_format, warnings, extensions, source,
                     previous_revision_id, registered_at, endpoint_count, diff_stats)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                incoming.getId(),
                serviceName,
                environment,
                seq,
                incoming.getSpecHash(),
                snapshot.getTitle(),
                snapshot.getVersion(),
                incoming.getAdapterType(),
                incoming.getSpecFormat(),
                toJson(incoming.getWarnings()),
                toJson(incoming.getExtensions()),
                SOURCE_REGISTER,
                previousId,
                now,
                diff.totalEndpoints(),
                toJson(diff));
    }

    private long nextRevisionSeq(String serviceName, String environment) {
        Long max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(revision_seq), 0) FROM service_snapshot_revisions WHERE service_name = ? AND environment = ?",
                Long.class,
                serviceName, environment);
        return (max == null ? 0 : max) + 1;
    }

    @Override
    public Optional<StoredRegistration> findCurrent(String serviceName, String environment) {
        String currentId = findCurrentRevisionId(serviceName, environment);
        if (currentId == null) {
            return Optional.empty();
        }
        return loadRevisionById(serviceName, environment, currentId, currentId);
    }

    @Override
    public Optional<StoredRegistration> findRevision(String serviceName, String environment, String revisionId) {
        String currentId = findCurrentRevisionId(serviceName, environment);
        return loadRevisionById(serviceName, environment, revisionId, currentId);
    }

    private String findCurrentRevisionId(String serviceName, String environment) {
        return jdbc.query(
                "SELECT current_revision_id FROM service_snapshots WHERE service_name = ? AND environment = ?",
                (rs, i) -> rs.getString(1),
                serviceName, environment).stream().findFirst().orElse(null);
    }

    private Optional<StoredRegistration> loadRevisionById(String serviceName, String environment,
                                                           String revisionId, String currentId) {
        List<StoredRegistration> rows = jdbc.query("""
                        SELECT id, service_name, environment, revision_seq, spec_hash, title, version,
                               adapter_type, spec_format, warnings, extensions, source,
                               registered_at
                        FROM service_snapshot_revisions
                        WHERE service_name = ? AND environment = ? AND id = ?
                        """,
                new RevisionRowMapper(currentId),
                serviceName, environment, revisionId);
        return rows.stream().findFirst();
    }

    @Override
    public List<RevisionSummary> listRevisions(String serviceName, String environment) {
        String currentId = jdbc.query(
                "SELECT current_revision_id FROM service_snapshots WHERE service_name = ? AND environment = ?",
                (rs, i) -> rs.getString(1),
                serviceName, environment).stream().findFirst().orElse(null);

        return jdbc.query("""
                        SELECT id, service_name, environment, revision_seq, spec_hash, title, version,
                               adapter_type, source, warnings, registered_at, endpoint_count, diff_stats
                        FROM service_snapshot_revisions
                        WHERE service_name = ? AND environment = ?
                        ORDER BY revision_seq DESC
                        """,
                (rs, i) -> {
                    RevisionDiff diff = fromJsonDiff(rs.getString("diff_stats"));
                    int added = diff == null ? 0 : diff.added().size();
                    int removed = diff == null ? 0 : diff.removed().size();
                    int modified = diff == null ? 0 : diff.modified().size();
                    int endpointCount = rs.getInt("endpoint_count");
                    boolean endpointCountNull = rs.wasNull();
                    return RevisionSummary.builder()
                            .id(rs.getString("id"))
                            .serviceName(rs.getString("service_name"))
                            .environment(rs.getString("environment"))
                            .seq(rs.getLong("revision_seq"))
                            .specHash(rs.getString("spec_hash"))
                            .title(rs.getString("title"))
                            .version(rs.getString("version"))
                            .adapterType(rs.getString("adapter_type"))
                            .source(rs.getString("source"))
                            .warningsCount(fromJsonList(rs.getString("warnings")).size())
                            .registeredAt(rs.getTimestamp("registered_at").toInstant())
                            .current(rs.getString("id").equals(currentId))
                            .endpointCount(endpointCountNull ? null : endpointCount)
                            .addedCount(diff == null ? null : added)
                            .removedCount(diff == null ? null : removed)
                            .modifiedCount(diff == null ? null : modified)
                            .build();
                },
                serviceName, environment);
    }

    @Override
    @Transactional
    public StoredRegistration activateRevision(String serviceName, String environment, String revisionId) {
        String currentId = findCurrentRevisionId(serviceName, environment);
        if (currentId == null) {
            throw new RegistrationNotFoundException(serviceName, environment);
        }
        StoredRegistration target = loadRevisionById(serviceName, environment, revisionId, currentId)
                .orElseThrow(() -> new RevisionNotFoundException(serviceName, environment, revisionId));

        if (target.isCurrent()) {
            return target;
        }

        CanonicalServiceSnapshot snapshot = target.getSnapshot();
        Timestamp now = Timestamp.from(Instant.now());
        // 同步 current pointer 以及冗余字段，catalog 读取 service_snapshots 时直接命中目标 revision
        int updated = jdbc.update("""
                        UPDATE service_snapshots
                        SET current_revision_id = ?, id = ?, title = ?, version = ?,
                            adapter_type = ?, spec_format = ?, spec_hash = ?,
                            warnings = ?, extensions = ?, registered_at = ?, last_seen_at = ?
                        WHERE service_name = ? AND environment = ?
                        """,
                target.getId(),
                target.getId(),
                snapshot.getTitle(),
                snapshot.getVersion(),
                target.getAdapterType(),
                target.getSpecFormat(),
                target.getSpecHash(),
                toJson(target.getWarnings()),
                toJson(target.getExtensions()),
                Timestamp.from(snapshot.getUpdatedAt()),
                now,
                serviceName, environment);

        if (updated == 0) {
            throw new RegistrationNotFoundException(serviceName, environment);
        }

        log.info("Activated revision {} for {} ({})", target.getId(), serviceName, environment);
        return target.toBuilder().current(true).build();
    }

    @Override
    public Collection<StoredRegistration> findAll() {
        List<String[]> pointers = jdbc.query(
                "SELECT service_name, environment, current_revision_id FROM service_snapshots WHERE current_revision_id IS NOT NULL ORDER BY service_name, environment",
                (rs, i) -> new String[] {
                        rs.getString("service_name"),
                        rs.getString("environment"),
                        rs.getString("current_revision_id")
                });
        return pointers.stream()
                .map(row -> loadRevisionById(row[0], row[1], row[2], row[2]).orElse(null))
                .filter(r -> r != null)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByRef(String serviceName, String environment) {
        List<String[]> revisions = jdbc.query(
                "SELECT id, spec_format FROM service_snapshot_revisions WHERE service_name = ? AND environment = ?",
                (rs, i) -> new String[] { rs.getString("id"), rs.getString("spec_format") },
                serviceName, environment);
        if (revisions.isEmpty() && findCurrent(serviceName, environment).isEmpty()) {
            return;
        }

        jdbc.update("DELETE FROM mcp_endpoints WHERE service_name = ? AND environment = ?",
                serviceName, environment);
        jdbc.update("DELETE FROM tag_order WHERE service_name = ? AND environment = ?",
                serviceName, environment);
        jdbc.update("DELETE FROM service_snapshots WHERE service_name = ? AND environment = ?",
                serviceName, environment);
        jdbc.update("DELETE FROM service_snapshot_revisions WHERE service_name = ? AND environment = ?",
                serviceName, environment);

        for (String[] rev : revisions) {
            fileSpecStore.delete(rev[0], rev[1]);
        }
        log.info("Deleted {} revision(s) for {} ({})", revisions.size(), serviceName, environment);
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

    private RevisionDiff fromJsonDiff(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RevisionDiff.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize diff_stats JSON, returning null", e);
            return null;
        }
    }

    /**
     * 行映射器：从 revision 表读元数据，从文件加载 snapshot 和 rawSpec，并基于 currentId 标记 current 标志。
     */
    private class RevisionRowMapper implements RowMapper<StoredRegistration> {

        private final String currentId;

        RevisionRowMapper(String currentId) {
            this.currentId = currentId;
        }

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
                    .revisionSeq(rs.getLong("revision_seq"))
                    .source(rs.getString("source"))
                    .registeredAt(rs.getTimestamp("registered_at").toInstant())
                    .current(id.equals(currentId))
                    .build();
        }
    }
}
