package ai.apiprism.adapter.starter;

import ai.apiprism.adapter.starter.exceptions.ApiPrismRegistrationException;
import ai.apiprism.adapter.starter.inspection.ApiPrismMappingInspector;
import ai.apiprism.adapter.starter.registration.ApiPrismRegistrationClient;
import ai.apiprism.adapter.starter.openapi.ApiPrismOpenApiSupplier;
import ai.apiprism.adapter.starter.openapi.HttpOpenApiSupplier;
import ai.apiprism.adapter.starter.openapi.SpringDocOpenApiSupplier;
import ai.apiprism.adapter.starter.registration.ApiPrismRegistrationListener;
import ai.apiprism.adapter.starter.registration.RegistrationRequestFactory;
import ai.apiprism.adapter.starter.registration.ServiceMetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ApiPrismProperties.class)
public class ApiPrismAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ApiPrismAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ApiPrismRegistrationClient apiPrismRegistrationClient(
            ApiPrismProperties properties,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider
    ) {
        ApiPrismProperties.HttpClient httpClientConfig = properties.getHttpClient();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(httpClientConfig.getConnectTimeoutMs());
        requestFactory.setReadTimeout(httpClientConfig.getReadTimeoutMs());

        RestClient.Builder restClientBuilder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
        return new ApiPrismRegistrationClient(restClientBuilder.requestFactory(requestFactory).build());
    }

    // 当 SpringDoc 在类路径时，优先使用进程内获取 OpenAPI 文档
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springdoc.webmvc.api.OpenApiResource")
    static class SpringDocOpenApiSupplierConfiguration {

        @Bean
        @ConditionalOnMissingBean(ApiPrismOpenApiSupplier.class)
        ApiPrismOpenApiSupplier springDocOpenApiSupplier(
                ApiPrismProperties properties,
                ObjectProvider<org.springdoc.webmvc.api.OpenApiResource> openApiResourceProvider
        ) {
            log.info("SpringDoc detected on classpath, enabling in-process OpenAPI document access");
            return new SpringDocOpenApiSupplier(properties, openApiResourceProvider);
        }
    }

    // 回退：无 SpringDoc 时通过 HTTP 获取 OpenAPI 文档
    @Bean
    @ConditionalOnMissingBean(ApiPrismOpenApiSupplier.class)
    public ApiPrismOpenApiSupplier httpOpenApiSupplier(ApiPrismProperties properties) {
        log.info("SpringDoc not detected on classpath, using HTTP fallback for OpenAPI document retrieval from {}",
                properties.getOpenapiPath());
        return new HttpOpenApiSupplier(properties);
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
    @ConditionalOnMissingBean(name = "apiPrismRetryTemplate")
    public RetryTemplate apiPrismRetryTemplate(ApiPrismProperties properties) {
        ApiPrismProperties.Retry retryConfig = properties.getRetry();

        if (!retryConfig.isEnabled()) {
            return RetryTemplate.builder().maxAttempts(1).noBackoff().build();
        }

        // 自定义重试策略：仅对 retryable 异常重试，4xx 等不可重试异常立即终止
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                retryConfig.getMaxAttempts(),
                Map.of(ApiPrismRegistrationException.class, true)
        ) {
            @Override
            public boolean canRetry(RetryContext context) {
                Throwable lastException = context.getLastThrowable();
                if (lastException instanceof ApiPrismRegistrationException ex && !ex.isRetryable()) {
                    return false;
                }
                return super.canRetry(context);
            }
        };

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryConfig.getInitialIntervalMs());
        backOffPolicy.setMultiplier(retryConfig.getMultiplier());
        backOffPolicy.setMaxInterval(retryConfig.getMaxIntervalMs());

        int maxAttempts = retryConfig.getMaxAttempts();
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                    RetryContext context,
                    org.springframework.retry.RetryCallback<T, E> callback,
                    Throwable throwable
            ) {
                log.warn("APIPrism registration attempt {}/{} failed: {}",
                        context.getRetryCount(), maxAttempts, throwable.getMessage());
            }
        });

        return retryTemplate;
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
            RegistrationRequestFactory requestFactory,
            RetryTemplate apiPrismRetryTemplate,
            Environment environment
    ) {
        return new ApiPrismRegistrationListener(
                properties, registrationClient, openApiSupplier,
                mappingInspector, metadataResolver, requestFactory,
                apiPrismRetryTemplate, environment
        );
    }
}
