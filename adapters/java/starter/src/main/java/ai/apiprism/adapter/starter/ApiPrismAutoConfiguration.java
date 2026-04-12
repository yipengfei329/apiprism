package ai.apiprism.adapter.starter;

import ai.apiprism.adapter.starter.inspection.ApiPrismMappingInspector;
import ai.apiprism.adapter.starter.registration.ApiPrismRegistrationClient;
import ai.apiprism.adapter.starter.openapi.ApiPrismOpenApiSupplier;
import ai.apiprism.adapter.starter.openapi.SpringDocOpenApiSupplier;
import ai.apiprism.adapter.starter.registration.ApiPrismRegistrationListener;
import ai.apiprism.adapter.starter.registration.RegistrationRequestFactory;
import ai.apiprism.adapter.starter.registration.ServiceMetadataResolver;
import org.springdoc.webmvc.api.OpenApiResource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@AutoConfiguration
@ConditionalOnClass(WebServerApplicationContext.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ApiPrismProperties.class)
public class ApiPrismAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApiPrismRegistrationClient apiPrismRegistrationClient(
            ObjectProvider<RestClient.Builder> restClientBuilderProvider
    ) {
        RestClient.Builder restClientBuilder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
        return new ApiPrismRegistrationClient(restClientBuilder.build());
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiPrismOpenApiSupplier apiPrismOpenApiSupplier(
            ApiPrismProperties properties,
            ObjectProvider<OpenApiResource> openApiResourceProvider
    ) {
        return new SpringDocOpenApiSupplier(properties, openApiResourceProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiPrismMappingInspector apiPrismMappingInspector(
            ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider
    ) {
        return new ApiPrismMappingInspector(handlerMappingProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceMetadataResolver serviceMetadataResolver(
            ApiPrismProperties properties,
            Environment environment
    ) {
        return new ServiceMetadataResolver(properties, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistrationRequestFactory registrationRequestFactory(
            ApiPrismProperties properties
    ) {
        return new RegistrationRequestFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "apiprism", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ApiPrismRegistrationListener apiPrismRegistrationListener(
            ApiPrismProperties properties,
            ApiPrismRegistrationClient registrationClient,
            ApiPrismOpenApiSupplier openApiSupplier,
            ApiPrismMappingInspector mappingInspector,
            ServiceMetadataResolver metadataResolver,
            RegistrationRequestFactory requestFactory
    ) {
        return new ApiPrismRegistrationListener(
                properties, registrationClient, openApiSupplier,
                mappingInspector, metadataResolver, requestFactory
        );
    }
}
