package ai.apiprism.mcp.event;

import org.springframework.context.ApplicationEvent;

/**
 * 服务注册或更新时发布的事件，触发 MCP 工具定义刷新。
 */
public class ServiceRegisteredEvent extends ApplicationEvent {

    private final String serviceName;
    private final String environment;

    public ServiceRegisteredEvent(Object source, String serviceName, String environment) {
        super(source);
        this.serviceName = serviceName;
        this.environment = environment;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getEnvironment() {
        return environment;
    }
}
