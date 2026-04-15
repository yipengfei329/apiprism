package ai.apiprism.mcp.routing;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * MCP 网关调度 Servlet：注册在 /mcp/* 路径下，根据 URL 路径中的服务名、环境、
 * 可选分组和端点类型分发请求到对应的 SSE 或 Streamable HTTP 传输层。
 *
 * <p>URL 格式:
 * <ul>
 *   <li>服务级（全部 API）: /mcp/{service}/{env}/sse|message|mcp</li>
 *   <li>分组级（单组 API）: /mcp/{service}/{env}/{groupSlug}/sse|message|mcp</li>
 * </ul>
 */
public class McpGatewayServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(McpGatewayServlet.class);

    /** 保留的端点名，不能作为 groupSlug */
    private static final Set<String> TRANSPORT_ENDPOINTS = Set.of("sse", "message", "mcp");

    private final PerServiceMcpRouter router;

    public McpGatewayServlet(PerServiceMcpRouter router) {
        this.router = router;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Missing service path. Use /mcp/{service}/{env}/sse or /mcp/{service}/{env}/{group}/sse");
            return;
        }

        PathParts parts = parsePath(pathInfo);
        if (parts == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Invalid MCP path. Expected: /mcp/{service}/{env}[/{group}]/sse|message|mcp");
            return;
        }

        String basePath = req.getContextPath() + req.getServletPath()
                + "/" + parts.serviceName + "/" + parts.environment
                + (parts.groupSlug != null ? "/" + parts.groupSlug : "");

        PerServiceMcpRouter.ServerEntry entry = router.getOrCreateEntry(
                parts.serviceName, parts.environment, parts.groupSlug, basePath);

        if (entry == null) {
            String target = parts.groupSlug != null
                    ? "Group '" + parts.groupSlug + "' in service " + parts.serviceName
                    : "Service " + parts.serviceName;
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    target + " (" + parts.environment + ") not found");
            return;
        }

        // 包装请求，让内部传输层看到正确的路径
        HttpServletRequest wrapped = new HttpServletRequestWrapper(req) {
            @Override
            public String getPathInfo() {
                return parts.endpoint;
            }

            @Override
            public String getServletPath() {
                return "";
            }

            @Override
            public String getRequestURI() {
                return parts.endpoint;
            }
        };

        // 根据端点类型分发到 SSE 或 Streamable HTTP 传输层
        if (parts.endpoint.equals("/sse") || parts.endpoint.equals("/message")) {
            entry.sseTransport().service(wrapped, resp);
        } else if (parts.endpoint.equals("/mcp")) {
            entry.streamableTransport().service(wrapped, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Unknown MCP endpoint: " + parts.endpoint);
        }
    }

    /**
     * 解析路径，支持服务级和分组级两种格式。
     *
     * <p>3 段: /{service}/{env}/{endpoint}        → 服务级，groupSlug=null
     * <p>4 段: /{service}/{env}/{groupSlug}/{endpoint} → 分组级
     *
     * <p>{endpoint} 必须是 sse / message / mcp 之一。如果第三段不是端点名，
     * 则视为 groupSlug，第四段作为端点。
     */
    static PathParts parsePath(String pathInfo) {
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        String[] segments = trimmed.split("/");

        if (segments.length < 3) {
            return null;
        }

        String serviceName = segments[0];
        String environment = segments[1];

        if (serviceName.isEmpty() || environment.isEmpty()) {
            return null;
        }

        // 3 段: service/env/endpoint（服务级）
        if (segments.length == 3) {
            String endpointName = segments[2];
            if (!TRANSPORT_ENDPOINTS.contains(endpointName)) {
                return null;
            }
            return new PathParts(serviceName, environment, null, "/" + endpointName);
        }

        // 4 段: service/env/groupSlug/endpoint（分组级）
        if (segments.length == 4) {
            String thirdSegment = segments[2];
            String fourthSegment = segments[3];

            // 如果第三段是端点名，说明路径格式不对（多了一段）
            if (TRANSPORT_ENDPOINTS.contains(thirdSegment)) {
                return null;
            }

            if (!TRANSPORT_ENDPOINTS.contains(fourthSegment)) {
                return null;
            }

            return new PathParts(serviceName, environment, thirdSegment, "/" + fourthSegment);
        }

        return null;
    }

    /**
     * @param groupSlug 分组标识，null 表示服务级（全部 API）
     * @param endpoint  传输端点，如 /sse、/message、/mcp
     */
    record PathParts(String serviceName, String environment, String groupSlug, String endpoint) {}
}
