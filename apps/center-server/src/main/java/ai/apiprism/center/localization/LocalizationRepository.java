package ai.apiprism.center.localization;

import io.hypersistence.tsid.TSID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * content_localizations 访问层。(entity_type, entity_id, field, locale, source)
 * 唯一，adapter 与 center 行并存，读取时按优先级合并（见 LocalizationResolver）。
 */
@Repository
public class LocalizationRepository {

    private final JdbcTemplate jdbc;

    public LocalizationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(LocalizationEntity entity,
                       String entityId,
                       String field,
                       String locale,
                       LocalizationSource source,
                       String content) {
        // content 为 null 或空表示删除该行，语义更直白
        if (content == null || content.isEmpty()) {
            jdbc.update("""
                    DELETE FROM content_localizations
                     WHERE entity_type = ? AND entity_id = ? AND field = ? AND locale = ? AND source = ?
                    """,
                    entity.code(), entityId, field, locale, source.code());
            return;
        }
        Timestamp now = Timestamp.from(Instant.now());
        int updated = jdbc.update("""
                UPDATE content_localizations
                   SET content = ?, updated_at = ?
                 WHERE entity_type = ? AND entity_id = ? AND field = ? AND locale = ? AND source = ?
                """,
                content, now, entity.code(), entityId, field, locale, source.code());
        if (updated > 0) {
            return;
        }
        try {
            jdbc.update("""
                    INSERT INTO content_localizations
                        (id, entity_type, entity_id, field, locale, source, content, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    TSID.Factory.getTsid().toString(), entity.code(), entityId, field,
                    locale, source.code(), content, now);
        } catch (DuplicateKeyException race) {
            jdbc.update("""
                    UPDATE content_localizations
                       SET content = ?, updated_at = ?
                     WHERE entity_type = ? AND entity_id = ? AND field = ? AND locale = ? AND source = ?
                    """,
                    content, now, entity.code(), entityId, field, locale, source.code());
        }
    }

    public List<LocalizationRow> findByEntity(LocalizationEntity entity, String entityId) {
        return jdbc.query("""
                SELECT entity_type, entity_id, field, locale, source, content, updated_at
                  FROM content_localizations
                 WHERE entity_type = ? AND entity_id = ?
                """, MAPPER, entity.code(), entityId);
    }

    /**
     * 批量拉取多个 (entity_type, entity_id) 下的所有行。按 entity_type 分组发多条 IN 查询，
     * 避免单条查询跨多种 entity_type 时 planner 失效。
     */
    public Map<EntityKey, List<LocalizationRow>> findByEntities(Collection<EntityKey> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> byType = new HashMap<>();
        for (EntityKey key : keys) {
            byType.computeIfAbsent(key.entityType().code(), k -> new ArrayList<>()).add(key.entityId());
        }
        Map<EntityKey, List<LocalizationRow>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : byType.entrySet()) {
            List<String> ids = entry.getValue();
            String placeholders = String.join(",", ids.stream().map(x -> "?").toList());
            Object[] args = new Object[ids.size() + 1];
            args[0] = entry.getKey();
            for (int i = 0; i < ids.size(); i++) {
                args[i + 1] = ids.get(i);
            }
            List<LocalizationRow> rows = jdbc.query("""
                    SELECT entity_type, entity_id, field, locale, source, content, updated_at
                      FROM content_localizations
                     WHERE entity_type = ? AND entity_id IN (""" + placeholders + ")",
                    MAPPER, args);
            for (LocalizationRow row : rows) {
                EntityKey key = new EntityKey(
                        LocalizationEntityLookup.fromCode(row.entityType()),
                        row.entityId());
                result.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }
        }
        return result;
    }

    public void deleteByEntity(LocalizationEntity entity, String entityId) {
        jdbc.update("DELETE FROM content_localizations WHERE entity_type = ? AND entity_id = ?",
                entity.code(), entityId);
    }

    public record EntityKey(LocalizationEntity entityType, String entityId) {
    }

    public record LocalizationRow(
            String entityType,
            String entityId,
            String field,
            String locale,
            String source,
            String content,
            Instant updatedAt) {
    }

    private static final class LocalizationEntityLookup {
        static LocalizationEntity fromCode(String code) {
            for (LocalizationEntity e : LocalizationEntity.values()) {
                if (e.code().equals(code)) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Unknown entity_type: " + code);
        }
    }

    private static final RowMapper<LocalizationRow> MAPPER = (rs, i) -> new LocalizationRow(
            rs.getString("entity_type"),
            rs.getString("entity_id"),
            rs.getString("field"),
            rs.getString("locale"),
            rs.getString("source"),
            rs.getString("content"),
            rs.getTimestamp("updated_at").toInstant());
}
