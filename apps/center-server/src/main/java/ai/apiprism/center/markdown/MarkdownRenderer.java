package ai.apiprism.center.markdown;

import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MarkdownRenderer {

    public String renderService(CanonicalServiceSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(snapshot.getTitle()).append('\n').append('\n');
        builder.append("- Service: `").append(snapshot.getRef().getName()).append("`\n");
        builder.append("- Environment: `").append(snapshot.getRef().getEnvironment()).append("`\n");
        builder.append("- Version: `").append(snapshot.getVersion()).append("`\n").append('\n');
        builder.append("## Groups\n\n");
        snapshot.getGroups().forEach(group -> builder
                .append("- `").append(group.getName()).append("`: ")
                .append(group.getDescription() == null ? "No description." : group.getDescription())
                .append('\n'));
        return builder.toString();
    }

    public String renderGroup(CanonicalServiceSnapshot snapshot, CanonicalGroup group) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(snapshot.getTitle()).append(" / ").append(group.getName()).append('\n').append('\n');
        if (group.getDescription() != null && !group.getDescription().isBlank()) {
            builder.append(group.getDescription()).append('\n').append('\n');
        }
        builder.append("## Operations\n\n");
        group.getOperations().forEach(operation -> builder
                .append("- `").append(operation.getMethod()).append(' ').append(operation.getPath()).append("`")
                .append(" (`").append(operation.getOperationId()).append("`)")
                .append(operation.getSummary() == null ? "" : ": " + operation.getSummary())
                .append('\n'));
        return builder.toString();
    }

    public String renderOperation(CanonicalServiceSnapshot snapshot, CanonicalOperation operation) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(operation.getOperationId()).append('\n').append('\n');
        builder.append("- Service: `").append(snapshot.getRef().getName()).append("`\n");
        builder.append("- Environment: `").append(snapshot.getRef().getEnvironment()).append("`\n");
        builder.append("- Method: `").append(operation.getMethod()).append("`\n");
        builder.append("- Path: `").append(operation.getPath()).append("`\n");
        if (operation.getSummary() != null && !operation.getSummary().isBlank()) {
            builder.append("- Summary: ").append(operation.getSummary()).append('\n');
        }
        if (operation.getDescription() != null && !operation.getDescription().isBlank()) {
            builder.append("- Description: ").append(operation.getDescription()).append('\n');
        }
        if (!operation.getSecurityRequirements().isEmpty()) {
            builder.append("- Security: ").append(String.join(", ", operation.getSecurityRequirements())).append('\n');
        }
        builder.append('\n').append("## Parameters\n\n");
        if (operation.getParameters().isEmpty()) {
            builder.append("No parameters.\n");
        } else {
            operation.getParameters().forEach(parameter -> builder.append(renderParameter(parameter)));
        }

        builder.append('\n').append("## Request Body\n\n");
        if (operation.getRequestBody() == null) {
            builder.append("No request body.\n");
        } else {
            builder.append("- Content-Type: `").append(operation.getRequestBody().getContentType()).append("`\n");
            builder.append("- Required: `").append(operation.getRequestBody().isRequired()).append("`\n");
            builder.append("- Schema: `").append(schemaTypeLabel(operation.getRequestBody().getSchema())).append("`\n");
            renderSchemaProperties(builder, operation.getRequestBody().getSchema());
        }

        builder.append('\n').append("## Responses\n\n");
        if (operation.getResponses().isEmpty()) {
            builder.append("No responses declared.\n");
        } else {
            operation.getResponses().forEach(response -> builder.append(renderResponse(response)));
        }
        return builder.toString();
    }

    private String renderParameter(CanonicalParameter parameter) {
        StringBuilder builder = new StringBuilder();
        builder.append("- `").append(parameter.getLocation()).append(':').append(parameter.getName()).append('`');
        String typeLabel = schemaTypeLabel(parameter.getSchema());
        if (typeLabel != null) {
            builder.append(" type=`").append(typeLabel).append('`');
        }
        builder.append(" required=`").append(parameter.isRequired()).append('`');
        if (parameter.getDescription() != null && !parameter.getDescription().isBlank()) {
            builder.append(' ').append(parameter.getDescription());
        }
        return builder.append('\n').toString();
    }

    private String renderResponse(CanonicalResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("- `").append(response.getStatusCode()).append("`");
        if (response.getDescription() != null) {
            builder.append(" ").append(response.getDescription());
        }
        if (response.getContentType() != null) {
            String typeLabel = schemaTypeLabel(response.getSchema());
            builder.append(" (`").append(response.getContentType()).append("`");
            if (typeLabel != null) {
                builder.append(", `").append(typeLabel).append("`");
            }
            builder.append(')');
        }
        builder.append('\n');
        renderSchemaProperties(builder, response.getSchema());
        return builder.toString();
    }

    /**
     * 从 JSON Schema map 中提取类型摘要标签。
     */
    @SuppressWarnings("unchecked")
    private String schemaTypeLabel(Map<String, Object> schema) {
        if (schema == null) return null;
        String type = (String) schema.get("type");
        String format = (String) schema.get("format");
        if ("array".equals(type) && schema.get("items") instanceof Map<?, ?> items) {
            return "array<" + schemaTypeLabel((Map<String, Object>) items) + ">";
        }
        if (type != null && format != null) return type + ":" + format;
        if (type != null) return type;
        return "object";
    }

    /**
     * 当 schema 含 properties 时，渲染 markdown 属性表格。
     */
    @SuppressWarnings("unchecked")
    private void renderSchemaProperties(StringBuilder builder, Map<String, Object> schema) {
        if (schema == null) return;
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        if (properties == null || properties.isEmpty()) return;
        List<String> required = schema.get("required") instanceof List<?> list
                ? list.stream().map(Object::toString).toList()
                : List.of();

        builder.append("\n  | Field | Type | Required | Description |\n");
        builder.append("  |-------|------|----------|-------------|\n");
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Map<String, Object> prop = (Map<String, Object>) entry.getValue();
            String name = entry.getKey();
            String typeLabel = schemaTypeLabel(prop);
            boolean isRequired = required.contains(name);
            String desc = prop.get("description") != null ? prop.get("description").toString() : "";
            builder.append("  | ").append(name)
                    .append(" | ").append(typeLabel != null ? typeLabel : "")
                    .append(" | ").append(isRequired ? "Yes" : "No")
                    .append(" | ").append(desc)
                    .append(" |\n");
        }
        builder.append('\n');
    }
}
