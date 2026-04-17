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

    /**
     * 适配器文档的默认语言（BCP-47，如 zh-CN、en-US）。
     * 中心据此把 summary/description 写入 content_localizations 的对应 locale 行。
     * 留空时中心回落全局默认（当前为 zh-CN）。
     */
    private String defaultLocale;

    /** 注册重试策略配置。 */
    private Retry retry = new Retry();

    /** 注册 HTTP 客户端配置。 */
    private HttpClient httpClient = new HttpClient();

    /**
     * 注册请求的指数退避重试策略。
     * <p>
     * 前 5 分钟内以较高频率重试（3s → 6s → 12s → …），之后逐步退避至最大 30 分钟间隔。
     * 仅对可重试异常（5xx、连接/超时错误）生效，4xx 等客户端错误不会重试。
     */
    @Data
    public static class Retry {

        /** 是否启用重试（默认启用）。 */
        private boolean enabled = true;

        /** 最大尝试次数（含首次调用，默认 15）。 */
        private int maxAttempts = 15;

        /** 初始退避间隔，毫秒（默认 3000）。 */
        private long initialIntervalMs = 3000;

        /** 退避倍率（默认 2.0）。 */
        private double multiplier = 2.0;

        /** 最大退避间隔，毫秒（默认 1800000，即 30 分钟）。 */
        private long maxIntervalMs = 1800000;
    }

    /**
     * 注册 HTTP 客户端超时配置。
     */
    @Data
    public static class HttpClient {

        /** 连接超时，毫秒（默认 5000）。 */
        private int connectTimeoutMs = 5000;

        /** 读取超时，毫秒（默认 10000）。 */
        private int readTimeoutMs = 10000;
    }
}
