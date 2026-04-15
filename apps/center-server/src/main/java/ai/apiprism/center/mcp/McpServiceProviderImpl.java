package ai.apiprism.center.mcp;

import ai.apiprism.center.repository.RegistrationRepository;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.mcp.spi.McpServiceProvider;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MCP 网关引擎的服务数据提供者实现，委托注册中心存储层获取服务快照。
 */
@Component
public class McpServiceProviderImpl implements McpServiceProvider {

    private final RegistrationRepository repository;

    public McpServiceProviderImpl(RegistrationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CanonicalServiceSnapshot> getServiceSnapshot(String serviceName, String environment) {
        return repository.findByRef(serviceName, environment)
                .map(StoredRegistration::getSnapshot);
    }

    @Override
    public List<ServiceRef> listAvailableServices() {
        return repository.findAll().stream()
                .map(reg -> reg.getSnapshot().getRef())
                .toList();
    }
}
