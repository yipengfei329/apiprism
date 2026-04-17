package ai.apiprism.center.catalog;

import ai.apiprism.center.localization.AcceptLanguageParser;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<ServiceCatalogItem> listServices(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        return catalogService.listServices(AcceptLanguageParser.pickPrimary(acceptLanguage));
    }

    @GetMapping("/services/{service}/env/{environment}")
    public CanonicalServiceSnapshot getService(
            @PathVariable String service,
            @PathVariable String environment,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        return catalogService.getService(service, environment, AcceptLanguageParser.pickPrimary(acceptLanguage));
    }

    @GetMapping("/services/{service}/env/{environment}/groups/{group}")
    public CanonicalGroup getGroup(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String group,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        return catalogService.getGroupBySlug(service, environment, group,
                AcceptLanguageParser.pickPrimary(acceptLanguage));
    }

    @GetMapping("/services/{service}/env/{environment}/operations/{operationId}")
    public CanonicalOperation getOperation(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String operationId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        return catalogService.getOperation(service, environment, operationId,
                AcceptLanguageParser.pickPrimary(acceptLanguage));
    }
}
