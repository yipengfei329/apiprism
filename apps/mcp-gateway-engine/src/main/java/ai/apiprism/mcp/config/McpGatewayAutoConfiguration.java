package ai.apiprism.mcp.config;

import ai.apiprism.mcp.converter.McpToolConverter;
import ai.apiprism.mcp.event.McpServiceRefreshListener;
import ai.apiprism.mcp.forward.HttpForwardingService;
import ai.apiprism.mcp.routing.McpGatewayServlet;
import ai.apiprism.mcp.routing.PerServiceMcpRouter;
import ai.apiprism.mcp.schema.McpToolSchemaBuilder;
import ai.apiprism.mcp.spi.McpServiceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * MCP 网关引擎自动配置。
 * 当宿主应用提供 {@link McpServiceProvider} Bean 且 apiprism.mcp.enabled=true 时自动激活。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "apiprism.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(McpServiceProvider.class)
@EnableConfigurationProperties(McpGatewayProperties.class)
public class McpGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public McpToolSchemaBuilder mcpToolSchemaBuilder() {
        return new McpToolSchemaBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public McpToolConverter mcpToolConverter(McpToolSchemaBuilder schemaBuilder) {
        return new McpToolConverter(schemaBuilder);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mcpForwardingRestClient")
    public RestClient mcpForwardingRestClient(McpGatewayProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getForwardConnectTimeoutMs());
        factory.setReadTimeout(properties.getForwardReadTimeoutMs());
        return RestClient.builder().requestFactory(factory).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpForwardingService httpForwardingService(RestClient mcpForwardingRestClient, ObjectMapper objectMapper) {
        return new HttpForwardingService(mcpForwardingRestClient, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public PerServiceMcpRouter perServiceMcpRouter(
            McpServiceProvider serviceProvider,
            McpToolConverter toolConverter,
            HttpForwardingService forwardingService,
            McpGatewayProperties properties
    ) {
        return new PerServiceMcpRouter(serviceProvider, toolConverter, forwardingService, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpGatewayServlet mcpGatewayServlet(PerServiceMcpRouter router) {
        return new McpGatewayServlet(router);
    }

    @Bean
    public ServletRegistrationBean<McpGatewayServlet> mcpGatewayServletRegistration(McpGatewayServlet servlet) {
        ServletRegistrationBean<McpGatewayServlet> registration = new ServletRegistrationBean<>(servlet, "/mcp/*");
        registration.setName("mcpGatewayServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public McpServiceRefreshListener mcpServiceRefreshListener(PerServiceMcpRouter router) {
        return new McpServiceRefreshListener(router);
    }
}
