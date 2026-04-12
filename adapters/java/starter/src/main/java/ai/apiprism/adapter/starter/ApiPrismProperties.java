package ai.apiprism.adapter.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * APIPrism 适配器配置项。
 * <p>
 * 必填：{@code apiprism.center-url}、{@code apiprism.project-name}。
 * 环境标识优先使用 {@code apiprism.environment}，未设置时自动读取
 * {@code spring.profiles.active} 的第一个 profile，均无则回退到 {@code "default"}。
 */
@Data
@ConfigurationProperties(prefix = "apiprism")
public class ApiPrismProperties {

    /** 是否启用 APIPrism 适配器（默认启用）。 */
    private boolean enabled = true;

    /** 应用就绪后是否立即注册（默认启用）。 */
    private boolean registerOnStartup = true;

    /** APIPrism center-server 地址，必须显式配置。 */
    private URI centerUrl = URI.create("http://localhost:8080");

    /**
     * 项目名称，用于在 center 目录中唯一标识本服务。
     * 建议在 {@code application.yml} 中显式配置；
     * 未配置时回退到 {@code spring.application.name}，并输出 WARN 日志。
     */
    private String projectName;

    /**
     * 环境标识（dev / staging / prod 等）。
     * 未配置时自动读取 {@code spring.profiles.active} 的第一个 profile；
     * 既无配置也无 profile 时回退到 {@code "default"}。
     */
    private String env;

    /**
     * 本服务对外可访问的 URL 列表，供 center 目录展示给使用方。
     * 未配置时回退到应用启动时自动检测的本地地址。
     */
    private List<String> serverUrls = new ArrayList<>();

    /** OpenAPI 规格文档路径（默认 {@code /v3/api-docs}）。 */
    private String openapiPath = "/v3/api-docs";
}
