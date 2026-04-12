package ai.apiprism.adapter.starter.openapi;

import ai.apiprism.adapter.starter.ApiPrismProperties;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.AbstractOpenApiResource;
import org.springdoc.webmvc.api.OpenApiResource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * {@link ApiPrismOpenApiSupplier} 的 SpringDoc 实现。
 * <p>
 * 优先通过反射直接从 SpringDoc Bean 读取 OpenAPI 模型（进程内，避免额外 HTTP 请求）；
 * 若 Bean 不可用或反射失败，则回退到本地 HTTP 请求指定的 OpenAPI 路径。
 */
public class SpringDocOpenApiSupplier implements ApiPrismOpenApiSupplier {

    private static final Logger log = LoggerFactory.getLogger(SpringDocOpenApiSupplier.class);
    private static final Method GET_OPEN_API_METHOD = initGetOpenApiMethod();

    private final ApiPrismProperties properties;
    private final ObjectProvider<OpenApiResource> openApiResourceProvider;

    public SpringDocOpenApiSupplier(ApiPrismProperties properties, ObjectProvider<OpenApiResource> openApiResourceProvider) {
        this.properties = properties;
        this.openApiResourceProvider = openApiResourceProvider;
    }

    @Override
    public ApiPrismOpenApiDocument fetch(String localBaseUrl) {
        OpenApiResource openApiResource = openApiResourceProvider.orderedStream().findFirst().orElse(null);
        if (openApiResource != null) {
            try {
                return buildDocument(loadInProcess(openApiResource), "springdoc-bean");
            } catch (ReflectiveOperationException | RuntimeException exception) {
                log.debug("Falling back to local OpenAPI HTTP fetch because in-process springdoc access failed: {}",
                        exception.getMessage());
            }
        }
        return fetchViaHttp(localBaseUrl);
    }

    private ApiPrismOpenApiDocument fetchViaHttp(String localBaseUrl) {
        String openapiPath = properties.getOpenapiPath();
        String content = RestClient.create()
                .get()
                .uri(localBaseUrl + openapiPath)
                .retrieve()
                .body(String.class);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("OpenAPI document endpoint returned an empty body.");
        }

        String format = detectFormat();
        OpenAPI openApi = parse(content, format);
        return ApiPrismOpenApiDocument.builder()
                .format(format)
                .content(content)
                .openApi(openApi)
                .operationCount(countOperations(openApi))
                .source("self-http")
                .build();
    }

    private ApiPrismOpenApiDocument buildDocument(OpenAPI openApi, String source) {
        String format = detectFormat();
        String content = "openapi-yaml".equals(format) ? Yaml.pretty(openApi) : Json.pretty(openApi);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("Unable to serialize OpenAPI document.");
        }
        return ApiPrismOpenApiDocument.builder()
                .format(format)
                .content(content)
                .openApi(openApi)
                .operationCount(countOperations(openApi))
                .source(source)
                .build();
    }

    private OpenAPI loadInProcess(OpenApiResource openApiResource) throws ReflectiveOperationException {
        return (OpenAPI) GET_OPEN_API_METHOD.invoke(openApiResource, Locale.getDefault());
    }

    private OpenAPI parse(String content, String format) {
        try {
            return "openapi-yaml".equals(format)
                    ? Yaml.mapper().readValue(content, OpenAPI.class)
                    : Json.mapper().readValue(content, OpenAPI.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse OpenAPI document from " + properties.getOpenapiPath(), exception);
        }
    }

    private String detectFormat() {
        return properties.getOpenapiPath().endsWith(".yaml") ? "openapi-yaml" : "openapi-json";
    }

    private int countOperations(OpenAPI openApi) {
        if (openApi == null || openApi.getPaths() == null) {
            return 0;
        }
        return openApi.getPaths().values().stream()
                .filter(pathItem -> pathItem != null)
                .mapToInt(pathItem -> pathItem.readOperations().size())
                .sum();
    }

    private static Method initGetOpenApiMethod() {
        try {
            Method method = AbstractOpenApiResource.class.getDeclaredMethod("getOpenApi", Locale.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to access springdoc OpenAPI builder.", exception);
        }
    }
}
