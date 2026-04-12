package ai.apiprism.adapter.starter.openapi;

/**
 * 策略接口：从运行中的应用获取 OpenAPI 文档。
 * 默认实现为 {@link SpringDocOpenApiSupplier}，支持用户通过声明自定义 Bean 替换。
 */
public interface ApiPrismOpenApiSupplier {

    ApiPrismOpenApiDocument fetch(String localBaseUrl);
}
