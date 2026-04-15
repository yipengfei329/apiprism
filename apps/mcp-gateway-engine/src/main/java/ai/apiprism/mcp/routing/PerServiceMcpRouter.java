package ai.apiprism.mcp.routing;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.mcp.config.McpGatewayProperties;
import ai.apiprism.mcp.converter.McpToolConverter;
import ai.apiprism.mcp.forward.HttpForwardingService;
import ai.apiprism.mcp.spi.McpServiceProvider;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每服务 MCP 路由管理器：为每个 service+environment 组合维护独立的 McpSyncServer 和传输层。
 */
public class PerServiceMcpRouter {

    private static final Logger log = LoggerFactory.getLogger(PerServiceMcpRouter.class);

    private final ConcurrentHashMap<String, ServerEntry> serverCache = new ConcurrentHashMap<>();

    private final McpServiceProvider serviceProvider;
    private final McpToolConverter toolConverter;
    private final HttpForwardingService forwardingService;
    private final McpGatewayProperties properties;

    public PerServiceMcpRouter(
            McpServiceProvider serviceProvider,
            McpToolConverter toolConverter,
            HttpForwardingService forwardingService,
            McpGatewayProperties properties
    ) {
        this.serviceProvider = serviceProvider;
        this.toolConverter = toolConverter;
        this.forwardingService = forwardingService;
        this.properties = properties;
    }

    /**
     * 获取或创建指定服务的传输层提供者。懒创建：首次访问时从 SPI 加载快照并构建 MCP 服务端。
     *
     * @return ServerEntry 包含传输层和服务端，若服务不存在则返回 null
     */
    public ServerEntry getOrCreateEntry(String serviceName, String environment, String basePath) {
        String key = cacheKey(serviceName, environment);
        ServerEntry existing = serverCache.get(key);
        if (existing != null) {
            return existing;
        }

        return serviceProvider.getServiceSnapshot(serviceName, environment)
                .map(snapshot -> serverCache.computeIfAbsent(key, k -> createEntry(snapshot, basePath)))
                .orElse(null);
    }

    /**
     * 获取缓存中已有的 entry（不触发创建）。
     */
    public ServerEntry getCachedEntry(String serviceName, String environment) {
        return serverCache.get(cacheKey(serviceName, environment));
    }

    /**
     * 服务重新注册后刷新 MCP 工具定义。
     */
    public void refreshServer(String serviceName, String environment) {
        String key = cacheKey(serviceName, environment);
        ServerEntry old = serverCache.remove(key);
        if (old != null) {
            log.info("Evicting MCP server for service {} ({}) due to re-registration", serviceName, environment);
            try {
                old.server().close();
            } catch (Exception e) {
                log.warn("Error closing old MCP server for {} ({})", serviceName, environment, e);
            }
        }
    }

    /**
     * 关闭所有 MCP 服务端。
     */
    public void closeAll() {
        serverCache.forEach((key, entry) -> {
            try {
                entry.server().close();
            } catch (Exception e) {
                log.warn("Error closing MCP server for {}", key, e);
            }
        });
        serverCache.clear();
    }

    private ServerEntry createEntry(CanonicalServiceSnapshot snapshot, String basePath) {
        String serviceName = snapshot.getRef().getName();
        String environment = snapshot.getRef().getEnvironment();

        log.info("Creating MCP server for service {} ({}) with {} tools",
                serviceName, environment, countOperations(snapshot));

        // 创建传输层
        HttpServletSseServerTransportProvider transport = HttpServletSseServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(new com.fasterxml.jackson.databind.ObjectMapper()))
                .baseUrl(basePath)
                .sseEndpoint("/sse")
                .messageEndpoint("/message")
                .build();

        // 转换操作为工具定义
        List<McpServerFeatures.SyncToolSpecification> toolSpecs = buildToolSpecs(snapshot);

        // 构建 McpSyncServer
        McpSyncServer server = McpServer.sync(transport)
                .serverInfo(properties.getServerName(), properties.getServerVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(toolSpecs)
                .build();

        return new ServerEntry(transport, server, snapshot);
    }

    private List<McpServerFeatures.SyncToolSpecification> buildToolSpecs(CanonicalServiceSnapshot snapshot) {
        return snapshot.getGroups().stream()
                .flatMap(group -> group.getOperations().stream())
                .map(operation -> new McpServerFeatures.SyncToolSpecification(
                        toolConverter.convertOperation(operation),
                        (exchange, request) -> handleToolCall(snapshot, operation, request)
                ))
                .toList();
    }

    private McpSchema.CallToolResult handleToolCall(
            CanonicalServiceSnapshot snapshot,
            CanonicalOperation operation,
            McpSchema.CallToolRequest request
    ) {
        Map<String, Object> arguments = request.arguments() != null ? request.arguments() : Map.of();
        return forwardingService.forward(snapshot, operation, arguments);
    }

    private long countOperations(CanonicalServiceSnapshot snapshot) {
        return snapshot.getGroups().stream()
                .mapToLong(g -> g.getOperations().size())
                .sum();
    }

    private String cacheKey(String serviceName, String environment) {
        return serviceName + "::" + environment;
    }

    /**
     * 缓存条目：包含传输层、MCP 服务端和对应的快照。
     */
    public record ServerEntry(
            HttpServletSseServerTransportProvider transport,
            McpSyncServer server,
            CanonicalServiceSnapshot snapshot
    ) {}
}
