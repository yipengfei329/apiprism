package ai.apiprism.mcp.routing;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * MCP 网关调度 Servlet：注册在 /mcp/* 路径下，根据 URL 路径中的服务名、环境和端点类型
 * 分发请求到对应服务的 SSE 或 Streamable HTTP 传输层。
 *
 * <p>URL 格式:
 * <ul>
 *   <li>SSE: /mcp/{serviceName}/{environment}/sse (GET) 和 /mcp/{serviceName}/{environment}/message (POST)</li>
 *   <li>Streamable HTTP: /mcp/{serviceName}/{environment}/mcp (GET/POST/DELETE)</li>
 * </ul>
 */
public class McpGatewayServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(McpGatewayServlet.class);

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
                    "Missing service path. Use /mcp/{service}/{env}/sse or /mcp/{service}/{env}/mcp");
            return;
        }

        PathParts parts = parsePath(pathInfo);
        if (parts == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Invalid MCP path. Expected: /mcp/{service}/{env}/sse|message|mcp");
            return;
        }

        String basePath = req.getContextPath() + req.getServletPath()
                + "/" + parts.serviceName + "/" + parts.environment;

        PerServiceMcpRouter.ServerEntry entry = router.getOrCreateEntry(
                parts.serviceName, parts.environment, basePath);

        if (entry == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Service not found: " + parts.serviceName + " (" + parts.environment + ")");
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
                    "Unknown MCP endpoint: " + parts.endpoint
                            + ". Use /sse, /message, or /mcp");
        }
    }

    /**
     * 解析路径: /serviceName/environment/endpoint
     * 例如: /demo-service/dev/sse → PathParts("demo-service", "dev", "/sse")
     *       /demo-service/dev/mcp → PathParts("demo-service", "dev", "/mcp")
     */
    static PathParts parsePath(String pathInfo) {
        // 去掉前导斜杠
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        // 至少需要 service/env/endpoint
        int firstSlash = trimmed.indexOf('/');
        if (firstSlash < 0) {
            return null;
        }
        String serviceName = trimmed.substring(0, firstSlash);

        String rest = trimmed.substring(firstSlash + 1);
        int secondSlash = rest.indexOf('/');
        if (secondSlash < 0) {
            return null;
        }
        String environment = rest.substring(0, secondSlash);
        String endpoint = rest.substring(secondSlash); // 保留前导斜杠

        if (serviceName.isEmpty() || environment.isEmpty() || endpoint.isEmpty()) {
            return null;
        }

        return new PathParts(serviceName, environment, endpoint);
    }

    record PathParts(String serviceName, String environment, String endpoint) {}
}
