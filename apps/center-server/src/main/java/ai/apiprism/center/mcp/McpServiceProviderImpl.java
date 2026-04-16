package ai.apiprism.center.mcp;

import ai.apiprism.center.catalog.CatalogService;
import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.mcp.spi.McpServiceProvider;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MCP 网关引擎的服务数据提供者实现，委托 CatalogService 获取服务快照（含 slug 补全），
 * 委托 McpEndpointRepository 检查端点启用状态。
 */
@Component
public class McpServiceProviderImpl implements McpServiceProvider {

    private final CatalogService catalogService;
    private final McpEndpointRepository mcpEndpointRepository;

    public McpServiceProviderImpl(CatalogService catalogService,
                                  McpEndpointRepository mcpEndpointRepository) {
        this.catalogService = catalogService;
        this.mcpEndpointRepository = mcpEndpointRepository;
    }

    @Override
    public Optional<CanonicalServiceSnapshot> getServiceSnapshot(String serviceName, String environment) {
        try {
            return Optional.of(catalogService.getService(serviceName, environment));
        } catch (RegistrationNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<ServiceRef> listAvailableServices() {
        return catalogService.listServices().stream()
                .map(item -> ServiceRef.builder()
                        .name(item.getName())
                        .environment(item.getEnvironment())
                        .build())
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
