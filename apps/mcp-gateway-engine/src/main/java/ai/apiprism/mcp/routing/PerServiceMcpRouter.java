package ai.apiprism.mcp.routing;

import ai.apiprism.model.CanonicalGroup;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每服务 MCP 路由管理器：为每个 service+environment（+可选 group）组合
 * 维护独立的 SSE 和 Streamable HTTP 两套 McpSyncServer 及传输层。
 *
 * <p>缓存粒度：
 * <ul>
 *   <li>服务级: key = "service::env"，暴露该服务全部分组的所有 API</li>
 *   <li>分组级: key = "service::env::groupSlug"，只暴露该分组下的 API</li>
 * </ul>
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
     * 获取或创建指定服务（及可选分组）的 MCP 入口。
     *
     * @param groupSlug 分组标识，null 表示服务级（全部 API）
     * @return ServerEntry，若服务或分组不存在则返回 null
     */
    public ServerEntry getOrCreateEntry(String serviceName, String environment,
                                        String groupSlug, String basePath) {
        String key = cacheKey(serviceName, environment, groupSlug);
        ServerEntry existing = serverCache.get(key);
        if (existing != null) {
            return existing;
        }

        // 检查端点是否已在前端控制页面上启用
        if (!serviceProvider.isMcpEndpointEnabled(serviceName, environment, groupSlug)) {
            return null;
        }

        Optional<CanonicalServiceSnapshot> snapshotOpt =
                serviceProvider.getServiceSnapshot(serviceName, environment);
        if (snapshotOpt.isEmpty()) {
            return null;
        }

        CanonicalServiceSnapshot snapshot = snapshotOpt.get();

        // 分组级：校验分组是否存在
        if (groupSlug != null) {
            boolean groupExists = snapshot.getGroups().stream()
                    .anyMatch(g -> groupSlug.equals(g.getSlug()) || groupSlug.equals(g.getName()));
            if (!groupExists) {
                return null;
            }
        }

        return serverCache.computeIfAbsent(key, k -> createEntry(snapshot, groupSlug, basePath));
    }

    /**
     * 服务重新注册后刷新 MCP 工具定义。
     * 同时清除该服务下所有缓存条目（服务级 + 全部分组级）。
     */
    public void refreshServer(String serviceName, String environment) {
        String prefix = serviceName + "::" + environment;
        serverCache.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                log.info("Evicting MCP server [{}] due to re-registration", entry.getKey());
                closeEntry(entry.getValue());
                return true;
            }
            return false;
        });
    }

    /**
     * 关闭所有 MCP 服务端。
     */
    public void closeAll() {
        serverCache.forEach((key, entry) -> closeEntry(entry));
        serverCache.clear();
    }

    private ServerEntry createEntry(CanonicalServiceSnapshot snapshot, String groupSlug,
                                    String basePath) {
        String serviceName = snapshot.getRef().getName();
        String environment = snapshot.getRef().getEnvironment();

        // 确定要暴露的操作范围
        List<CanonicalOperation> operations = resolveOperations(snapshot, groupSlug);

        String scope = groupSlug != null
                ? "group '" + groupSlug + "' of service " + serviceName
                : "service " + serviceName;
        log.info("Creating MCP servers (SSE + Streamable HTTP) for {} ({}) with {} tools",
                scope, environment, operations.size());

        // 构建工具规格列表
        List<McpServerFeatures.SyncToolSpecification> toolSpecs = buildToolSpecs(snapshot, operations);
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

    /**
     * 根据 groupSlug 筛选操作。
     * groupSlug 为 null 返回全部操作；否则返回匹配分组下的操作（先匹配 slug，再 fallback 到 name）。
     */
    private List<CanonicalOperation> resolveOperations(CanonicalServiceSnapshot snapshot,
                                                        String groupSlug) {
        if (groupSlug == null) {
            return snapshot.getGroups().stream()
                    .flatMap(g -> g.getOperations().stream())
                    .toList();
        }

        return snapshot.getGroups().stream()
                .filter(g -> groupSlug.equals(g.getSlug()) || groupSlug.equals(g.getName()))
                .findFirst()
                .map(CanonicalGroup::getOperations)
                .orElse(List.of());
    }

    private List<McpServerFeatures.SyncToolSpecification> buildToolSpecs(
            CanonicalServiceSnapshot snapshot,
            List<CanonicalOperation> operations
    ) {
        return operations.stream()
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

    private String cacheKey(String serviceName, String environment, String groupSlug) {
        String key = serviceName + "::" + environment;
        if (groupSlug != null) {
            key += "::" + groupSlug;
        }
        return key;
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
