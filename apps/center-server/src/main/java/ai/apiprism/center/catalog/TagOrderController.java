package ai.apiprism.center.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/services/{service}/env/{environment}/tag-order")
public class TagOrderController {

    private final TagOrderRepository tagOrderRepository;
    private final CatalogService catalogService;

    public TagOrderController(TagOrderRepository tagOrderRepository, CatalogService catalogService) {
        this.tagOrderRepository = tagOrderRepository;
        this.catalogService = catalogService;
    }

    @GetMapping
    public Map<String, Integer> getOrder(
            @PathVariable String service,
            @PathVariable String environment
    ) {
        catalogService.getRegistration(service, environment);
        return tagOrderRepository.findOrder(service, environment);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveOrder(
            @PathVariable String service,
            @PathVariable String environment,
            @RequestBody TagOrderRequest request
    ) {
        if (request.slugs() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slugs must not be null");
        }
        catalogService.getRegistration(service, environment);
        tagOrderRepository.saveOrder(service, environment, request.slugs());
    }

    record TagOrderRequest(List<String> slugs) {}
}
