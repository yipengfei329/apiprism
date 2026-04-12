package ai.apiprism.center.markdown;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalServiceSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成面向 AI Agent 的接口 Markdown 文档。
 * 格式对齐 LLM function call 定义：operationId 作为 function name，
 * 参数合并为 JSON Schema，输出按状态码列出 schema，附带 curl 示例。
 */
@Component
public class AgentMarkdownRenderer {

    private final ObjectMapper objectMapper;

    public AgentMarkdownRenderer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 渲染单个接口的 Agent Markdown。
     */
    public String renderAgentOperation(CanonicalServiceSnapshot snapshot, CanonicalOperation operation) {
        StringBuilder md = new StringBuilder();

        // 标题：operationId
        md.append("# ").append(operation.getOperationId()).append('\n');

        // 摘要作为引用块
        if (operation.getSummary() != null && !operation.getSummary().isBlank()) {
            md.append('\n').append("> ").append(operation.getSummary()).append('\n');
        }

        // 详细描述
        if (operation.getDescription() != null && !operation.getDescription().isBlank()) {
            md.append('\n').append(operation.getDescription()).append('\n');
        }

        // Endpoint
        String baseUrl = resolveBaseUrl(snapshot);
        md.append("\n## Endpoint\n\n");
        md.append("`").append(operation.getMethod()).append(' ').append(operation.getPath()).append("`\n\n");
        md.append("Base URL: `").append(baseUrl).append("`\n");

        // Authentication
        if (!operation.getSecurityRequirements().isEmpty()) {
            md.append("\n## Authentication\n\n");
            md.append("`").append(String.join("`, `", operation.getSecurityRequirements())).append("`\n");
        }

        // Input Parameters
        if (!operation.getParameters().isEmpty()) {
            md.append("\n## Input Parameters\n\n");
            md.append("```json\n");
            md.append(toJson(buildInputParametersSchema(operation.getParameters())));
            md.append("\n```\n");
        }

        // Request Body
        if (operation.getRequestBody() != null) {
            md.append("\n## Request Body\n\n");
            if (operation.getRequestBody().getContentType() != null) {
                md.append("Content-Type: `").append(operation.getRequestBody().getContentType()).append("`\n\n");
            }
            if (operation.getRequestBody().getSchema() != null) {
                md.append("```json\n");
                md.append(toJson(operation.getRequestBody().getSchema()));
                md.append("\n```\n");
            }
        }

        // Output
        if (!operation.getResponses().isEmpty()) {
            md.append("\n## Output\n");
            for (CanonicalResponse response : operation.getResponses()) {
                md.append("\n### ").append(response.getStatusCode());
                if (response.getDescription() != null && !response.getDescription().isBlank()) {
                    md.append(' ').append(response.getDescription());
                }
                if (response.getContentType() != null) {
                    md.append(" (`").append(response.getContentType()).append("`)");
                }
                md.append('\n');
                if (response.getSchema() != null) {
                    md.append("\n```json\n");
                    md.append(toJson(response.getSchema()));
                    md.append("\n```\n");
                }
            }
        }

        // Example curl
        md.append("\n## Example\n\n");
        md.append(buildCurlExample(snapshot, operation));

        return md.toString();
    }

    /**
     * 将所有参数合并为一个 JSON Schema object，每个属性附加 in 和 required 字段。
     */
    private Map<String, Object> buildInputParametersSchema(List<CanonicalParameter> parameters) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> requiredList = new ArrayList<>();

        for (CanonicalParameter param : parameters) {
            Map<String, Object> prop = new LinkedHashMap<>();
            // 从原始 schema 复制 type/format 等字段
            if (param.getSchema() != null) {
                prop.putAll(param.getSchema());
            }
            if (param.getDescription() != null && !param.getDescription().isBlank()) {
                prop.put("description", param.getDescription());
            }
            prop.put("in", param.getLocation());
            prop.put("required", param.isRequired());
            properties.put(param.getName(), prop);

            if (param.isRequired()) {
                requiredList.add(param.getName());
            }
        }

        schema.put("properties", properties);
        if (!requiredList.isEmpty()) {
            schema.put("required", requiredList);
        }
        return schema;
    }

    /**
     * 生成 curl 调用示例。
     */
    private String buildCurlExample(CanonicalServiceSnapshot snapshot, CanonicalOperation operation) {
        StringBuilder curl = new StringBuilder();
        String baseUrl = resolveBaseUrl(snapshot);
        String method = operation.getMethod().toUpperCase();

        // 构建 URL：baseUrl + path，query 参数拼接
        StringBuilder url = new StringBuilder(baseUrl).append(operation.getPath());
        List<String> queryParams = new ArrayList<>();
        for (CanonicalParameter param : operation.getParameters()) {
            if ("query".equals(param.getLocation())) {
                queryParams.add(param.getName() + "=<" + param.getName() + ">");
            }
        }
        if (!queryParams.isEmpty()) {
            url.append('?').append(String.join("&", queryParams));
        }

        curl.append("```bash\n");
        curl.append("curl -X ").append(method).append(" '").append(url).append("'");

        // 认证头
        if (!operation.getSecurityRequirements().isEmpty()) {
            curl.append(" \\\n  -H 'Authorization: Bearer <TOKEN>'");
        }

        // 请求体
        if (operation.getRequestBody() != null && operation.getRequestBody().getSchema() != null) {
            if (operation.getRequestBody().getContentType() != null) {
                curl.append(" \\\n  -H 'Content-Type: ").append(operation.getRequestBody().getContentType()).append("'");
            }
            Object exampleData = ExampleGenerator.generate(operation.getRequestBody().getSchema());
            if (exampleData != null) {
                curl.append(" \\\n  -d '").append(toJson(exampleData)).append("'");
            }
        }

        curl.append("\n```\n");
        return curl.toString();
    }

    private String resolveBaseUrl(CanonicalServiceSnapshot snapshot) {
        if (snapshot.getServerUrls() != null && !snapshot.getServerUrls().isEmpty()) {
            return snapshot.getServerUrls().get(0);
        }
        return "https://<SERVICE_HOST>";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
