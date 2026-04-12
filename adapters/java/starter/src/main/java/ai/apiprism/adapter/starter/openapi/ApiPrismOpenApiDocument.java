package ai.apiprism.adapter.starter.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Builder;
import lombok.Getter;

/**
 * 从应用加载的 OpenAPI 文档，包含格式、原始内容、解析后的模型、操作数量及来源标识。
 */
@Getter
@Builder
public class ApiPrismOpenApiDocument {

    /** OpenAPI 格式标识（如 "openapi" 或 "swagger"） */
    private final String format;

    /** 原始 OpenAPI 文档内容（JSON 或 YAML 字符串） */
    private final String content;

    /** 解析后的 OpenAPI 模型 */
    private final OpenAPI openApi;

    /** 文档中的操作总数 */
    private final int operationCount;

    /** 文档来源标识 */
    private final String source;
}
