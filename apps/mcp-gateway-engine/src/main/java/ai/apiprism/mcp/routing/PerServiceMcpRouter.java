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
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每服务 MCP 路由管理器：为每个 service+environment 组合维护独立的
 * SSE 和 Streamable HTTP 两套 McpSyncServer 及传输层。
 *
 * <p>SSE 端点：/mcp/{service}/{env}/sse + /message
 * <p>Streamable HTTP 端点：/mcp/{service}/{env}/mcp (GET/POST/DELETE)
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
     * 获取或创建指定服务的 MCP 入口。懒创建：首次访问时从 SPI 加载快照并构建双协议 MCP 服务端。
     *
     * @return ServerEntry 包含 SSE 和 Streamable HTTP 两套传输层，若服务不存在则返回 null
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
     * 服务重新注册后刷新 MCP 工具定义。
     */
    public void refreshServer(String serviceName, String environment) {
        String key = cacheKey(serviceName, environment);
        ServerEntry old = serverCache.remove(key);
        if (old != null) {
            log.info("Evicting MCP servers for service {} ({}) due to re-registration", serviceName, environment);
            closeEntry(old);
        }
    }

    /**
     * 关闭所有 MCP 服务端。
     */
    public void closeAll() {
        serverCache.forEach((key, entry) -> closeEntry(entry));
        serverCache.clear();
    }

    private ServerEntry createEntry(CanonicalServiceSnapshot snapshot, String basePath) {
        String serviceName = snapshot.getRef().getName();
        String environment = snapshot.getRef().getEnvironment();
        long toolCount = countOperations(snapshot);

        log.info("Creating MCP servers (SSE + Streamable HTTP) for service {} ({}) with {} tools",
                serviceName, environment, toolCount);

        // 构建工具规格列表（SSE 和 Streamable HTTP 共用同一组工具定义）
        List<McpServerFeatures.SyncToolSpecification> toolSpecs = buildToolSpecs(snapshot);
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(
                new com.fasterxml.jackson.databind.ObjectMapper());

        // --- SSE 传输 ---
        HttpServletSseServerTransportProvider sseTransport = HttpServletSseServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .baseUrl(basePath)
                .sseEndpoint("/sse")
                .messageEndpoint("/message")
                .build();

        McpSyncServer sseServer = McpServer.sync(sseTransport)
                .serverInfo(properties.getServerName(), properties.getServerVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(toolSpecs)
                .build();

        // --- Streamable HTTP 传输 ---
        HttpServletStreamableServerTransportProvider streamableTransport =
                HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(jsonMapper)
                        .mcpEndpoint("/mcp")
                        .build();

        McpSyncServer streamableServer = McpServer.sync(streamableTransport)
                .serverInfo(properties.getServerName(), properties.getServerVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(toolSpecs)
                .build();

        return new ServerEntry(sseTransport, sseServer, streamableTransport, streamableServer, snapshot);
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

    private void closeEntry(ServerEntry entry) {
        try {
            entry.sseServer().close();
        } catch (Exception e) {
            log.warn("Error closing SSE MCP server", e);
        }
        try {
            entry.streamableServer().close();
        } catch (Exception e) {
            log.warn("Error closing Streamable HTTP MCP server", e);
        }
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
     * 缓存条目：包含 SSE 和 Streamable HTTP 两套传输层、MCP 服务端及对应的快照。
     */
    public record ServerEntry(
            HttpServletSseServerTransportProvider sseTransport,
            McpSyncServer sseServer,
            HttpServletStreamableServerTransportProvider streamableTransport,
            McpSyncServer streamableServer,
            CanonicalServiceSnapshot snapshot
    ) {}
}
