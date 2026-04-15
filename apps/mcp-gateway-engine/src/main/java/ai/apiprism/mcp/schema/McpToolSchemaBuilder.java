package ai.apiprism.mcp.schema;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 CanonicalOperation 的参数与请求体合并为 MCP Tool 所需的 JSON Schema。
 *
 * <p>策略：path/query/header 参数作为顶层属性，请求体放在 "requestBody" 属性下。
 * 这样 LLM 调用工具时参数结构清晰，转发引擎可以无歧义地还原 HTTP 请求。</p>
 */
public class McpToolSchemaBuilder {

    /**
     * 为指定操作构建 MCP 工具输入 JSON Schema。
     */
    public McpSchema.JsonSchema buildInputSchema(CanonicalOperation operation) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> requiredList = new ArrayList<>();

        // 参数（path / query / header）作为顶层属性
        if (operation.getParameters() != null) {
            for (CanonicalParameter param : operation.getParameters()) {
                properties.put(param.getName(), buildParamProperty(param));
                if (param.isRequired()) {
                    requiredList.add(param.getName());
                }
            }
        }

        // 请求体放在 "requestBody" 属性
        CanonicalRequestBody body = operation.getRequestBody();
        if (body != null && body.getSchema() != null) {
            properties.put("requestBody", body.getSchema());
            if (body.isRequired()) {
                requiredList.add("requestBody");
            }
        }

        return new McpSchema.JsonSchema(
                "object",
                properties,
                requiredList.isEmpty() ? null : requiredList,
                null,
                null,
                null
        );
    }

    private Map<String, Object> buildParamProperty(CanonicalParameter param) {
        Map<String, Object> prop = new LinkedHashMap<>();
        // 复制原始 schema 中的 type/format/enum 等字段
        if (param.getSchema() != null) {
            prop.putAll(param.getSchema());
        }
        if (param.getDescription() != null && !param.getDescription().isBlank()) {
            prop.put("description", param.getDescription());
        }
        return prop;
    }
}
