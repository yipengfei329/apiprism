package ai.apiprism.center.markdown;

import ai.apiprism.center.catalog.CatalogService;
import ai.apiprism.center.config.CenterProperties;
import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI Agent 专用文档端点，URL 极简化，统一以 apidocs.md 命名。
 */
@RestController
@RequestMapping("/api/v1")
public class AgentDocsController {

    private final CatalogService catalogService;
    private final AgentMarkdownRenderer agentMarkdownRenderer;
    private final CenterProperties centerProperties;

    public AgentDocsController(CatalogService catalogService,
                               AgentMarkdownRenderer agentMarkdownRenderer,
                               CenterProperties centerProperties) {
        this.catalogService = catalogService;
        this.agentMarkdownRenderer = agentMarkdownRenderer;
        this.centerProperties = centerProperties;
    }

    @GetMapping(value = "/apidocs.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String globalCatalog(HttpServletRequest request) {
        String baseUrl = resolveBaseUrl(request);
        return agentMarkdownRenderer.renderGlobalCatalog(catalogService.listServices(), baseUrl);
    }

    @GetMapping(value = "/{service}/{env}/apidocs.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String serviceIndex(
            @PathVariable String service,
            @PathVariable String env,
            HttpServletRequest request
    ) {
        String baseUrl = resolveBaseUrl(request);
        CanonicalServiceSnapshot snapshot = catalogService.getService(service, env);
        return agentMarkdownRenderer.renderServiceIndex(snapshot, baseUrl);
    }

    /**
     * 统一的第三段 identifier 端点：先尝试按分组 slug 解析，不匹配再按 operationId 解析。
     * 这样分组和接口可以共用同一个 URL 模式 /{service}/{env}/{identifier}/apidocs.md。
     */
    @GetMapping(value = "/{service}/{env}/{identifier}/apidocs.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String groupOrOperationDocs(
            @PathVariable String service,
            @PathVariable String env,
            @PathVariable String identifier,
            HttpServletRequest request
    ) {
        String baseUrl = resolveBaseUrl(request);
        CanonicalServiceSnapshot snapshot = catalogService.getService(service, env);

        // 先尝试分组 slug
        try {
            CanonicalGroup group = catalogService.getGroupBySlug(service, env, identifier);
            return agentMarkdownRenderer.renderGroupIndex(snapshot, group, baseUrl);
        } catch (RegistrationNotFoundException ignored) {
            // identifier 不是分组 slug，继续尝试 operationId
        }

        // 再尝试 operationId
        CanonicalOperation operation = catalogService.getOperation(service, env, identifier);
        return agentMarkdownRenderer.renderAgentOperation(snapshot, operation);
    }

    /**
     * 解析 Center 对外根地址。
     * 优先使用配置 apiprism.center.external-url，
     * 未配置时从请求头自动推导（支持 X-Forwarded-* 反向代理头）。
     */
    private String resolveBaseUrl(HttpServletRequest request) {
        String configured = centerProperties.getExternalUrl();
        if (configured != null && !configured.isBlank()) {
            // 去掉尾部斜杠
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        // 从请求自动推导
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) scheme = request.getScheme();
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) host = request.getHeader("Host");
        if (host == null) host = request.getServerName() + ":" + request.getServerPort();
        return scheme + "://" + host;
    }
}
