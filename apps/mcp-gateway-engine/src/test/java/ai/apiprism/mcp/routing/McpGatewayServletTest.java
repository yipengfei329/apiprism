package ai.apiprism.mcp.routing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpGatewayServletTest {

    // --- 服务级路径（3 段） ---

    @Test
    void parsePath_serviceLevel_sse() {
        var parts = McpGatewayServlet.parsePath("/demo-service/dev/sse");

        assertNotNull(parts);
        assertEquals("demo-service", parts.serviceName());
        assertEquals("dev", parts.environment());
        assertNull(parts.groupSlug());
        assertEquals("/sse", parts.endpoint());
    }

    @Test
    void parsePath_serviceLevel_message() {
        var parts = McpGatewayServlet.parsePath("/demo-service/dev/message");

        assertNotNull(parts);
        assertNull(parts.groupSlug());
        assertEquals("/message", parts.endpoint());
    }

    @Test
    void parsePath_serviceLevel_mcp() {
        var parts = McpGatewayServlet.parsePath("/demo-service/prod/mcp");

        assertNotNull(parts);
        assertEquals("demo-service", parts.serviceName());
        assertEquals("prod", parts.environment());
        assertNull(parts.groupSlug());
        assertEquals("/mcp", parts.endpoint());
    }

    // --- 分组级路径（4 段） ---

    @Test
    void parsePath_groupLevel_sse() {
        var parts = McpGatewayServlet.parsePath("/demo-service/dev/users/sse");

        assertNotNull(parts);
        assertEquals("demo-service", parts.serviceName());
        assertEquals("dev", parts.environment());
        assertEquals("users", parts.groupSlug());
        assertEquals("/sse", parts.endpoint());
    }

    @Test
    void parsePath_groupLevel_message() {
        var parts = McpGatewayServlet.parsePath("/demo-service/dev/orders/message");

        assertNotNull(parts);
        assertEquals("orders", parts.groupSlug());
        assertEquals("/message", parts.endpoint());
    }

    @Test
    void parsePath_groupLevel_mcp() {
        var parts = McpGatewayServlet.parsePath("/demo-service/dev/notifications/mcp");

        assertNotNull(parts);
        assertEquals("notifications", parts.groupSlug());
        assertEquals("/mcp", parts.endpoint());
    }

    @Test
    void parsePath_groupLevel_chineseSlug() {
        var parts = McpGatewayServlet.parsePath("/demo-service/dev/yong-hu-guan-li/sse");

        assertNotNull(parts);
        assertEquals("yong-hu-guan-li", parts.groupSlug());
        assertEquals("/sse", parts.endpoint());
    }

    // --- 异常路径 ---

    @Test
    void parsePath_tooFewSegments() {
        assertNull(McpGatewayServlet.parsePath("/demo-service/dev"));
    }

    @Test
    void parsePath_tooManySegments() {
        assertNull(McpGatewayServlet.parsePath("/a/b/c/d/e"));
    }

    @Test
    void parsePath_invalidEndpoint_serviceLevel() {
        assertNull(McpGatewayServlet.parsePath("/demo-service/dev/unknown"));
    }

    @Test
    void parsePath_invalidEndpoint_groupLevel() {
        assertNull(McpGatewayServlet.parsePath("/demo-service/dev/users/unknown"));
    }

    @Test
    void parsePath_emptyServiceName() {
        assertNull(McpGatewayServlet.parsePath("//dev/sse"));
    }

    @Test
    void parsePath_rootOnly() {
        assertNull(McpGatewayServlet.parsePath("/"));
    }
}
