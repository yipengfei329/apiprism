package ai.apiprism.center.markdown;

import ai.apiprism.center.catalog.CatalogService;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MarkdownController {

    private final CatalogService catalogService;
    private final MarkdownRenderer markdownRenderer;
    private final AgentMarkdownRenderer agentMarkdownRenderer;

    public MarkdownController(CatalogService catalogService,
                              MarkdownRenderer markdownRenderer,
                              AgentMarkdownRenderer agentMarkdownRenderer) {
        this.catalogService = catalogService;
        this.markdownRenderer = markdownRenderer;
        this.agentMarkdownRenderer = agentMarkdownRenderer;
    }

    @GetMapping(value = "/services/{service}/env/{environment}/markdown.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String serviceMarkdown(
            @PathVariable String service,
            @PathVariable String environment
    ) {
        return markdownRenderer.renderService(catalogService.getService(service, environment));
    }

    @GetMapping(value = "/services/{service}/env/{environment}/groups/{group}/markdown.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String groupMarkdown(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String group
    ) {
        CanonicalServiceSnapshot snapshot = catalogService.getService(service, environment);
        CanonicalGroup canonicalGroup = catalogService.getGroup(service, environment, group);
        return markdownRenderer.renderGroup(snapshot, canonicalGroup);
    }

    @GetMapping(value = "/services/{service}/env/{environment}/operations/{operationId}/markdown.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String operationMarkdown(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String operationId
    ) {
        CanonicalServiceSnapshot snapshot = catalogService.getService(service, environment);
        CanonicalOperation operation = catalogService.getOperation(service, environment, operationId);
        return markdownRenderer.renderOperation(snapshot, operation);
    }

    @GetMapping(value = "/services/{service}/env/{environment}/operations/{operationId}/agent-markdown.md", produces = MediaType.TEXT_PLAIN_VALUE)
    public String agentOperationMarkdown(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String operationId
    ) {
        CanonicalServiceSnapshot snapshot = catalogService.getService(service, environment);
        CanonicalOperation operation = catalogService.getOperation(service, environment, operationId);
        return agentMarkdownRenderer.renderAgentOperation(snapshot, operation);
    }
}
