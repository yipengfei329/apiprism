package ai.apiprism.center.catalog;

import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.center.markdown.AgentMarkdownRenderer;
import ai.apiprism.center.markdown.MarkdownRenderer;
import ai.apiprism.center.registration.RegistrationService;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final RegistrationService registrationService;
    private final MarkdownRenderer markdownRenderer;
    private final AgentMarkdownRenderer agentMarkdownRenderer;

    public CatalogController(RegistrationService registrationService,
                             MarkdownRenderer markdownRenderer,
                             AgentMarkdownRenderer agentMarkdownRenderer) {
        this.registrationService = registrationService;
        this.markdownRenderer = markdownRenderer;
        this.agentMarkdownRenderer = agentMarkdownRenderer;
    }

    @GetMapping("/services")
    public List<ServiceCatalogItem> listServices() {
        return registrationService.listServices();
    }

    @GetMapping("/services/{service}/environments/{environment}")
    public CanonicalServiceSnapshot getService(
            @PathVariable String service,
            @PathVariable String environment
    ) {
        return registrationService.getService(service, environment);
    }

    @GetMapping("/services/{service}/environments/{environment}/groups/{group}")
    public CanonicalGroup getGroup(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String group
    ) {
        return registrationService.getService(service, environment).getGroups().stream()
                .filter(candidate -> candidate.getName().equals(group))
                .findFirst()
                .orElseThrow(() -> new RegistrationNotFoundException(service, environment));
    }

    @GetMapping("/services/{service}/environments/{environment}/operations/{operationId}")
    public CanonicalOperation getOperation(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String operationId
    ) {
        return registrationService.getService(service, environment).getGroups().stream()
                .flatMap(group -> group.getOperations().stream())
                .filter(operation -> operation.getOperationId().equals(operationId))
                .findFirst()
                .orElseThrow(() -> new RegistrationNotFoundException(service, environment));
    }

    @GetMapping(value = "/markdown/services/{service}/environments/{environment}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String serviceMarkdown(
            @PathVariable String service,
            @PathVariable String environment
    ) {
        return markdownRenderer.renderService(registrationService.getService(service, environment));
    }

    @GetMapping(value = "/markdown/services/{service}/environments/{environment}/groups/{group}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String groupMarkdown(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String group
    ) {
        CanonicalGroup canonicalGroup = getGroup(service, environment, group);
        return markdownRenderer.renderGroup(registrationService.getService(service, environment), canonicalGroup);
    }

    @GetMapping(value = "/markdown/services/{service}/environments/{environment}/operations/{operationId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String operationMarkdown(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String operationId
    ) {
        CanonicalServiceSnapshot snapshot = registrationService.getService(service, environment);
        CanonicalOperation operation = getOperation(service, environment, operationId);
        return markdownRenderer.renderOperation(snapshot, operation);
    }

    @GetMapping(value = "/agent-markdown/services/{service}/environments/{environment}/operations/{operationId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String agentOperationMarkdown(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String operationId
    ) {
        CanonicalServiceSnapshot snapshot = registrationService.getService(service, environment);
        CanonicalOperation operation = getOperation(service, environment, operationId);
        return agentMarkdownRenderer.renderAgentOperation(snapshot, operation);
    }
}
