package ai.apiprism.center.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * MCP 端点启用状态的持久化仓库。
 */
@Repository
public class McpEndpointRepository {

    private static final Logger log = LoggerFactory.getLogger(McpEndpointRepository.class);

    /** 服务级端点的 group_slug 占位值 */
    private static final String SERVICE_LEVEL = "";

    private final JdbcTemplate jdbc;

    public McpEndpointRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 设置服务级 MCP 开关。
     */
    public void setServiceEnabled(String serviceName, String environment, boolean enabled) {
        upsert(serviceName, environment, SERVICE_LEVEL, enabled);
    }

    /**
     * 设置分组级 MCP 开关。
     */
    public void setGroupEnabled(String serviceName, String environment, String groupSlug, boolean enabled) {
        upsert(serviceName, environment, groupSlug, enabled);
    }

    /**
     * 查询服务级 MCP 是否已启用。
     */
    public boolean isServiceEnabled(String serviceName, String environment) {
        return isEnabled(serviceName, environment, SERVICE_LEVEL);
    }

    /**
     * 查询分组级 MCP 是否已启用。
     */
    public boolean isGroupEnabled(String serviceName, String environment, String groupSlug) {
        return isEnabled(serviceName, environment, groupSlug);
    }

    /**
     * 查询指定服务下所有已启用 MCP 的分组 slug 列表（不含服务级）。
     */
    public List<String> listEnabledGroups(String serviceName, String environment) {
        return jdbc.queryForList(
                "SELECT group_slug FROM mcp_endpoints WHERE service_name = ? AND environment = ? AND group_slug <> '' AND enabled = TRUE",
                String.class,
                serviceName, environment);
    }

    private void upsert(String serviceName, String environment, String groupSlug, boolean enabled) {
        jdbc.update("""
                MERGE INTO mcp_endpoints (service_name, environment, group_slug, enabled, updated_at)
                KEY (service_name, environment, group_slug)
                VALUES (?, ?, ?, ?, ?)
                """,
                serviceName, environment, groupSlug, enabled, Timestamp.from(Instant.now()));
        log.info("MCP endpoint [{}] {} ({}) group='{}' set to {}",
                enabled ? "ENABLED" : "DISABLED", serviceName, environment,
                groupSlug.isEmpty() ? "<service-level>" : groupSlug, enabled);
    }

    private boolean isEnabled(String serviceName, String environment, String groupSlug) {
        List<Boolean> results = jdbc.queryForList(
                "SELECT enabled FROM mcp_endpoints WHERE service_name = ? AND environment = ? AND group_slug = ?",
                Boolean.class,
                serviceName, environment, groupSlug);
        return !results.isEmpty() && results.get(0);
    }
}
