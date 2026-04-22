package ai.apiprism.center.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分组排序的持久化仓库。
 * position 是 0-based 整数，值越小越靠前。
 */
@Repository
public class TagOrderRepository {

    private static final Logger log = LoggerFactory.getLogger(TagOrderRepository.class);

    private final JdbcTemplate jdbc;

    public TagOrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 返回 slug → position 映射。若该服务从未保存过顺序则返回空 Map。
     */
    public Map<String, Integer> findOrder(String serviceName, String environment) {
        List<Object[]> rows = jdbc.query(
                "SELECT group_slug, position FROM tag_order WHERE service_name = ? AND environment = ? ORDER BY position",
                (rs, i) -> new Object[]{ rs.getString("group_slug"), rs.getInt("position") },
                serviceName, environment);
        Map<String, Integer> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (Integer) row[1]);
        }
        return result;
    }

    /**
     * 整体替换某服务的分组顺序。slugs 列表的下标即为新 position。
     * 先 DELETE 旧行再 INSERT 新行，保证原子性。
     */
    public void saveOrder(String serviceName, String environment, List<String> slugs) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("DELETE FROM tag_order WHERE service_name = ? AND environment = ?",
                serviceName, environment);
        for (int i = 0; i < slugs.size(); i++) {
            jdbc.update(
                    "INSERT INTO tag_order (service_name, environment, group_slug, position, updated_at) VALUES (?, ?, ?, ?, ?)",
                    serviceName, environment, slugs.get(i), i, now);
        }
        log.info("Saved tag order for {} ({}) — {} slugs", serviceName, environment, slugs.size());
    }

    /**
     * 删除服务的所有排序记录（服务删除时调用）。
     */
    public void deleteByRef(String serviceName, String environment) {
        jdbc.update("DELETE FROM tag_order WHERE service_name = ? AND environment = ?",
                serviceName, environment);
    }
}
