package ai.apiprism.adapter.starter.registration;

import ai.apiprism.adapter.starter.ApiPrismProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * 从 {@link ApiPrismProperties} 和 Spring {@link Environment} 解析注册所需的服务元数据。
 * <p>
 * 解析规则：
 * <ul>
 *   <li>projectName：优先使用配置值，回退 {@code spring.application.name}，并输出 WARN</li>
 *   <li>environment：优先使用配置值，回退 {@code spring.profiles.active} 第一个，均无则 {@code "default"}</li>
 *   <li>serverUrls：优先使用配置列表，回退应用本地地址</li>
 * </ul>
 */
public class ServiceMetadataResolver {

    private static final Logger log = LoggerFactory.getLogger(ServiceMetadataResolver.class);

    private final ApiPrismProperties properties;
    private final Environment springEnvironment;

    public ServiceMetadataResolver(ApiPrismProperties properties, Environment springEnvironment) {
        this.properties = properties;
        this.springEnvironment = springEnvironment;
    }

    public ServiceMetadata resolve(String localBaseUrl) {
        String projectName = resolveProjectName();
        String environment = resolveEnvironment();
        List<String> serverUrls = resolveServerUrls(localBaseUrl);
        return new ServiceMetadata(projectName, environment, serverUrls);
    }

    private String resolveProjectName() {
        String projectName = properties.getProjectName();
        if (projectName != null && !projectName.isBlank()) {
            return projectName;
        }
        String fallback = springEnvironment.getProperty("spring.application.name", "application");
        log.warn("apiprism.project-name is not set; falling back to spring.application.name={}. "
                + "It is recommended to set apiprism.project-name explicitly.", fallback);
        return fallback;
    }

    private String resolveEnvironment() {
        String environment = properties.getEnv();
        if (environment != null && !environment.isBlank()) {
            return environment;
        }
        String[] activeProfiles = springEnvironment.getActiveProfiles();
        return activeProfiles.length > 0 ? activeProfiles[0] : "default";
    }

    private List<String> resolveServerUrls(String localBaseUrl) {
        List<String> configured = properties.getServerUrls();
        return configured.isEmpty() ? List.of(localBaseUrl) : configured;
    }
}
