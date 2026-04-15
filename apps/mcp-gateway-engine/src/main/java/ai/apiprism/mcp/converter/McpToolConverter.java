package ai.apiprism.mcp.converter;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.mcp.schema.McpToolSchemaBuilder;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * 将 CanonicalServiceSnapshot 中的操作转换为 MCP Tool 定义列表。
 */
public class McpToolConverter {

    private final McpToolSchemaBuilder schemaBuilder;

    public McpToolConverter(McpToolSchemaBuilder schemaBuilder) {
        this.schemaBuilder = schemaBuilder;
    }

    /**
     * 将服务快照中所有操作转换为 MCP Tool 列表。
     */
    public List<McpSchema.Tool> convertSnapshot(CanonicalServiceSnapshot snapshot) {
        return snapshot.getGroups().stream()
                .flatMap(group -> group.getOperations().stream())
                .map(this::convertOperation)
                .toList();
    }

    /**
     * 将单个操作转换为 MCP Tool。
     */
    public McpSchema.Tool convertOperation(CanonicalOperation operation) {
        return McpSchema.Tool.builder()
                .name(operation.getOperationId())
                .description(buildDescription(operation))
                .inputSchema(schemaBuilder.buildInputSchema(operation))
                .build();
    }

    /**
     * 拼接操作描述：method + path 作为首行，后接 summary 和 description。
     */
    private String buildDescription(CanonicalOperation operation) {
        StringBuilder desc = new StringBuilder();
        desc.append(operation.getMethod().toUpperCase()).append(' ').append(operation.getPath());

        if (operation.getSummary() != null && !operation.getSummary().isBlank()) {
            desc.append("\n\n").append(operation.getSummary());
        }
        if (operation.getDescription() != null && !operation.getDescription().isBlank()) {
            desc.append("\n\n").append(operation.getDescription());
        }
        return desc.toString();
    }
}
