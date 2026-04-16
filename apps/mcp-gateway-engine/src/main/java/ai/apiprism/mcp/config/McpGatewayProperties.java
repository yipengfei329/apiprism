package ai.apiprism.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 网关引擎配置属性。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "apiprism.mcp")
public class McpGatewayProperties {

    /** 是否启用 MCP 网关 */
    private boolean enabled = true;

    /** 转发 HTTP 请求的连接超时（毫秒） */
    private int forwardConnectTimeoutMs = 5000;

    /** 转发 HTTP 请求的读取超时（毫秒） */
    private int forwardReadTimeoutMs = 30000;

    /** MCP 服务端名称 */
    private String serverName = "apiprism-mcp-gateway";

    /** MCP 服务端版本 */
    private String serverVersion = "1.0.0";
}
