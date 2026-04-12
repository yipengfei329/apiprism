package ai.apiprism.center.catalog;

import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.center.repository.RegistrationRepository;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 目录查询服务：提供服务列表、快照查询、分组与接口检索能力。
 */
@Service
public class CatalogService {

    private final RegistrationRepository repository;

    public CatalogService(RegistrationRepository repository) {
        this.repository = repository;
    }

    public List<ServiceCatalogItem> listServices() {
        return repository.findAll().stream()
                .map(this::toCatalogItem)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
    }

    public CanonicalServiceSnapshot getService(String serviceName, String environment) {
        return require(serviceName, environment).getSnapshot();
    }

    public StoredRegistration getRegistration(String serviceName, String environment) {
        return require(serviceName, environment);
    }

    public CanonicalGroup getGroup(String serviceName, String environment, String groupName) {
        return getService(serviceName, environment).getGroups().stream()
                .filter(candidate -> candidate.getName().equals(groupName))
                .findFirst()
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    public CanonicalOperation getOperation(String serviceName, String environment, String operationId) {
        return getService(serviceName, environment).getGroups().stream()
                .flatMap(group -> group.getOperations().stream())
                .filter(operation -> operation.getOperationId().equals(operationId))
                .findFirst()
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    private StoredRegistration require(String serviceName, String environment) {
        return repository.findByRef(serviceName, environment)
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    private ServiceCatalogItem toCatalogItem(StoredRegistration registration) {
        CanonicalServiceSnapshot snapshot = registration.getSnapshot();
        return ServiceCatalogItem.builder()
                .name(snapshot.getRef().getName())
                .environment(snapshot.getRef().getEnvironment())
                .title(snapshot.getTitle())
                .version(snapshot.getVersion())
                .updatedAt(snapshot.getUpdatedAt())
                .groups(snapshot.getGroups().stream().map(CanonicalGroup::getName).toList())
                .build();
    }
}
