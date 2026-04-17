package ai.apiprism.center.registration.persistence;

import ai.apiprism.center.registration.persistence.PersistenceRows.TagRow;
import io.hypersistence.tsid.TSID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * tags + operation_tags 访问层。
 * tag 按 (service_id, name) 唯一；name 相同认为是同一个 tag，slug 可能被适配器改写。
 */
@Repository
public class TagRepository {

    private final JdbcTemplate jdbc;

    public TagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TagRow> findByServiceId(String serviceId) {
        return jdbc.query("SELECT id, service_id, name, slug FROM tags WHERE service_id = ? ORDER BY name",
                ROW_MAPPER, serviceId);
    }

    public Optional<TagRow> findByServiceAndName(String serviceId, String name) {
        return jdbc.query("SELECT id, service_id, name, slug FROM tags WHERE service_id = ? AND name = ?",
                ROW_MAPPER, serviceId, name).stream().findFirst();
    }

    /**
     * 为 service 批量 upsert tag，返回 name → id 索引。
     * slug 变化则更新；新 tag 自动分配 TSID。
     */
    public Map<String, String> upsertAll(String serviceId, Collection<TagSpec> tags) {
        Map<String, String> index = new HashMap<>();
        for (TagSpec spec : tags) {
            Optional<TagRow> existing = findByServiceAndName(serviceId, spec.name());
            if (existing.isPresent()) {
                if (spec.slug() != null && !spec.slug().equals(existing.get().slug())) {
                    jdbc.update("UPDATE tags SET slug = ? WHERE id = ?", spec.slug(), existing.get().id());
                }
                index.put(spec.name(), existing.get().id());
                continue;
            }
            String id = TSID.Factory.getTsid().toString();
            try {
                jdbc.update("INSERT INTO tags (id, service_id, name, slug) VALUES (?, ?, ?, ?)",
                        id, serviceId, spec.name(), spec.slug());
                index.put(spec.name(), id);
            } catch (DuplicateKeyException race) {
                index.put(spec.name(), findByServiceAndName(serviceId, spec.name()).orElseThrow().id());
            }
        }
        return index;
    }

    public List<String> findOperationTagIds(String operationId) {
        return jdbc.queryForList("SELECT tag_id FROM operation_tags WHERE operation_id = ?",
                String.class, operationId);
    }

    public List<TagRow> findTagsByOperationId(String operationId) {
        return jdbc.query("""
                SELECT t.id, t.service_id, t.name, t.slug
                  FROM tags t
                  JOIN operation_tags ot ON ot.tag_id = t.id
                 WHERE ot.operation_id = ?
                 ORDER BY t.name
                """, ROW_MAPPER, operationId);
    }

    public void replaceOperationTags(String operationId, Collection<String> tagIds) {
        List<String> current = findOperationTagIds(operationId);
        Set<String> target = new HashSet<>(tagIds);
        Set<String> existing = new HashSet<>(current);

        for (String gone : existing) {
            if (!target.contains(gone)) {
                jdbc.update("DELETE FROM operation_tags WHERE operation_id = ? AND tag_id = ?",
                        operationId, gone);
            }
        }
        for (String added : target) {
            if (!existing.contains(added)) {
                jdbc.update("INSERT INTO operation_tags (operation_id, tag_id) VALUES (?, ?)",
                        operationId, added);
            }
        }
    }

    public Map<String, List<String>> findTagIdsByOperationIds(Collection<String> operationIds) {
        if (operationIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", operationIds.stream().map(x -> "?").toList());
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT operation_id, tag_id FROM operation_tags WHERE operation_id IN (" + placeholders + ")",
                operationIds.toArray());
        Map<String, List<String>> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            grouped.computeIfAbsent((String) row.get("operation_id"), k -> new ArrayList<>())
                    .add((String) row.get("tag_id"));
        }
        return grouped;
    }

    /**
     * 简单值对象，承载适配器提交的 tag 名 + 预生成 slug。
     */
    public record TagSpec(String name, String slug) {
    }

    private static final RowMapper<TagRow> ROW_MAPPER = (rs, i) -> new TagRow(
            rs.getString("id"),
            rs.getString("service_id"),
            rs.getString("name"),
            rs.getString("slug"));
}
