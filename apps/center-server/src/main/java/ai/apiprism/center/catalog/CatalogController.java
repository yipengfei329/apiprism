package ai.apiprism.center.catalog;

import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/services")
    public List<ServiceCatalogItem> listServices() {
        return catalogService.listServices();
    }

    @GetMapping("/services/{service}/env/{environment}")
    public CanonicalServiceSnapshot getService(
            @PathVariable String service,
            @PathVariable String environment
    ) {
        return catalogService.getService(service, environment);
    }

    @GetMapping("/services/{service}/env/{environment}/groups/{group}")
    public CanonicalGroup getGroup(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String group
    ) {
        return catalogService.getGroupBySlug(service, environment, group);
    }

    @GetMapping("/services/{service}/env/{environment}/operations/{operationId}")
    public CanonicalOperation getOperation(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String operationId
    ) {
        return catalogService.getOperation(service, environment, operationId);
    }

    @GetMapping("/services/{service}/envs")
    public List<String> listEnvironments(@PathVariable String service) {
        return catalogService.listEnvironments(service);
    }

    @DeleteMapping("/services/{service}/env/{environment}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(
            @PathVariable String service,
            @PathVariable String environment
    ) {
        catalogService.deleteService(service, environment);
    }
}
