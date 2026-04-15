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
 * MCP 网关引擎的服务数据提供者实现，委托注册中心存储层获取服务快照，
 * 委托 McpEndpointRepository 检查端点启用状态。
 */
@Component
public class McpServiceProviderImpl implements McpServiceProvider {

    private final RegistrationRepository registrationRepository;
    private final McpEndpointRepository mcpEndpointRepository;

    public McpServiceProviderImpl(RegistrationRepository registrationRepository,
                                  McpEndpointRepository mcpEndpointRepository) {
        this.registrationRepository = registrationRepository;
        this.mcpEndpointRepository = mcpEndpointRepository;
    }

    @Override
    public Optional<CanonicalServiceSnapshot> getServiceSnapshot(String serviceName, String environment) {
        return registrationRepository.findByRef(serviceName, environment)
                .map(StoredRegistration::getSnapshot);
    }

    @Override
    public List<ServiceRef> listAvailableServices() {
        return registrationRepository.findAll().stream()
                .map(reg -> reg.getSnapshot().getRef())
                .toList();
    }

    @Override
    public boolean isMcpEndpointEnabled(String serviceName, String environment, String groupSlug) {
        if (groupSlug == null) {
            return mcpEndpointRepository.isServiceEnabled(serviceName, environment);
        }
        return mcpEndpointRepository.isGroupEnabled(serviceName, environment, groupSlug);
    }
}
