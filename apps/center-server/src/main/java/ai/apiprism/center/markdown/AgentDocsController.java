package ai.apiprism.center.markdown;

import ai.apiprism.center.catalog.CatalogService;
import ai.apiprism.center.config.CenterProperties;
import ai.apiprism.center.localization.AcceptLanguageParser;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public String globalCatalog(HttpServletRequest request,
                                @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        String baseUrl = resolveBaseUrl(request);
        String locale = AcceptLanguageParser.pickPrimary(acceptLanguage);
        return agentMarkdownRenderer.renderGlobalCatalog(catalogService.listServices(locale), baseUrl);
    }

    @GetMapping(value = "/{service}/{env}/apidocs.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String serviceIndex(
            @PathVariable String service,
            @PathVariable String env,
            HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String baseUrl = resolveBaseUrl(request);
        String locale = AcceptLanguageParser.pickPrimary(acceptLanguage);
        CanonicalServiceSnapshot snapshot = catalogService.getService(service, env, locale);
        return agentMarkdownRenderer.renderServiceIndex(snapshot, baseUrl);
    }

    @GetMapping(value = "/{service}/{env}/{operationId}/apidocs.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String operationDocs(
            @PathVariable String service,
            @PathVariable String env,
            @PathVariable String operationId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String locale = AcceptLanguageParser.pickPrimary(acceptLanguage);
        CanonicalServiceSnapshot snapshot = catalogService.getService(service, env, locale);
        CanonicalOperation operation = catalogService.getOperation(service, env, operationId, locale);
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
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) scheme = request.getScheme();
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) host = request.getHeader("Host");
        if (host == null) host = request.getServerName() + ":" + request.getServerPort();
        return scheme + "://" + host;
    }
}
