package ai.apiprism.adapter.starter.openapi;

import ai.apiprism.adapter.starter.ApiPrismProperties;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 通过 HTTP 请求本地 OpenAPI 端点获取文档的默认实现。
 * <p>
 * 不依赖 SpringDoc，适用于任何暴露标准 OpenAPI 端点的应用。
 * 当 SpringDoc 在类路径上时，{@link SpringDocOpenApiSupplier} 会覆盖此实现，
 * 优先通过进程内方式获取 OpenAPI 模型以避免额外的 HTTP 请求。
 */
public class HttpOpenApiSupplier implements ApiPrismOpenApiSupplier {

    private static final Logger log = LoggerFactory.getLogger(HttpOpenApiSupplier.class);

    protected final ApiPrismProperties properties;

    public HttpOpenApiSupplier(ApiPrismProperties properties) {
        this.properties = properties;
    }

    @Override
    public ApiPrismOpenApiDocument fetch(String localBaseUrl) {
        return fetchViaHttp(localBaseUrl);
    }

    protected final ApiPrismOpenApiDocument fetchViaHttp(String localBaseUrl) {
        String openapiPath = properties.getOpenapiPath();
        log.debug("Fetching OpenAPI document via HTTP from {}{}", localBaseUrl, openapiPath);

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

    protected final ApiPrismOpenApiDocument buildDocumentFromModel(OpenAPI openApi, String source) {
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

    protected final OpenAPI parse(String content, String format) {
        try {
            return "openapi-yaml".equals(format)
                    ? Yaml.mapper().readValue(content, OpenAPI.class)
                    : Json.mapper().readValue(content, OpenAPI.class);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to parse OpenAPI document from " + properties.getOpenapiPath(), exception);
        }
    }

    protected final String detectFormat() {
        return properties.getOpenapiPath().endsWith(".yaml") ? "openapi-yaml" : "openapi-json";
    }

    protected final int countOperations(OpenAPI openApi) {
        if (openApi == null || openApi.getPaths() == null) {
            return 0;
        }
        return openApi.getPaths().values().stream()
                .filter(pathItem -> pathItem != null)
                .mapToInt(pathItem -> pathItem.readOperations().size())
                .sum();
    }
}
