package ai.apiprism.center.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Center 服务配置属性。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "apiprism.center")
public class CenterProperties {

    /**
     * Center 对外可访问的根地址，用于在 Agent 文档中生成完整链接。
     * 示例: https://api.example.com
     * 未配置时自动从 HTTP 请求头推导。
     */
    private String externalUrl;
}
