package ai.apiprism.mcp.event;

import ai.apiprism.mcp.routing.PerServiceMcpRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

/**
 * 监听服务注册事件，触发对应 MCP 服务端实例的工具列表刷新。
 */
public class McpServiceRefreshListener {

    private static final Logger log = LoggerFactory.getLogger(McpServiceRefreshListener.class);

    private final PerServiceMcpRouter router;

    public McpServiceRefreshListener(PerServiceMcpRouter router) {
        this.router = router;
    }

    @EventListener
    public void onServiceRegistered(ServiceRegisteredEvent event) {
        log.info("Service registration event received for {} ({}), refreshing MCP tools",
                event.getServiceName(), event.getEnvironment());
        router.refreshServer(event.getServiceName(), event.getEnvironment());
    }
}
