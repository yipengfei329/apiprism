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
 * MCP 网关调度 Servlet：注册在 /mcp/* 路径下，根据 URL 路径中的服务名和环境
 * 分发请求到对应的 HttpServletSseServerTransportProvider 实例。
 *
 * <p>URL 格式: /mcp/{serviceName}/{environment}/sse 或 /mcp/{serviceName}/{environment}/message
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
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Missing service path. Use /mcp/{service}/{env}/sse");
            return;
        }

        PathParts parts = parsePath(pathInfo);
        if (parts == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Invalid MCP path format. Expected: /mcp/{serviceName}/{environment}/sse or /message");
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

        // 包装请求，让内部的 HttpServletSseServerTransportProvider 看到正确的路径
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

        // 委派给对应服务的传输层
        entry.transport().service(wrapped, resp);
    }

    /**
     * 解析路径: /serviceName/environment/endpoint
     * 例如: /demo-service/dev/sse → PathParts("demo-service", "dev", "/sse")
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
