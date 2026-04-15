package ai.apiprism.mcp.spi;

import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;

import java.util.List;
import java.util.Optional;

/**
 * 服务数据访问 SPI，解耦 MCP 网关引擎与具体的服务注册中心实现。
 * 由宿主应用（如 center-server）提供实现。
 */
public interface McpServiceProvider {

    /**
     * 获取指定服务的规范化快照。
     */
    Optional<CanonicalServiceSnapshot> getServiceSnapshot(String serviceName, String environment);

    /**
     * 列出所有可用服务的引用。
     */
    List<ServiceRef> listAvailableServices();

    /**
     * 检查指定的 MCP 端点是否已启用。
     *
     * @param groupSlug 分组标识，null 表示服务级
     * @return true 如果该端点已被用户启用
     */
    boolean isMcpEndpointEnabled(String serviceName, String environment, String groupSlug);
}
