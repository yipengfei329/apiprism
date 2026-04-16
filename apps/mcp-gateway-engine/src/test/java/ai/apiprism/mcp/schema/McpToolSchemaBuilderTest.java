package ai.apiprism.mcp.schema;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpToolSchemaBuilderTest {

    private final McpToolSchemaBuilder builder = new McpToolSchemaBuilder();

    @Test
    void buildInputSchema_noParamsNoBody_returnsEmptyObjectSchema() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("healthCheck")
                .method("GET")
                .path("/health")
                .build();

        McpSchema.JsonSchema schema = builder.buildInputSchema(op);

        assertEquals("object", schema.type());
        assertTrue(schema.properties().isEmpty());
        assertNull(schema.required());
    }

    @Test
    void buildInputSchema_withPathAndQueryParams() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("getUser")
                .method("GET")
                .path("/users/{userId}")
                .parameter(CanonicalParameter.builder()
                        .name("userId")
                        .location("path")
                        .required(true)
                        .schema(Map.of("type", "integer"))
                        .description("User identifier")
                        .build())
                .parameter(CanonicalParameter.builder()
                        .name("fields")
                        .location("query")
                        .required(false)
                        .schema(Map.of("type", "string"))
                        .description("Comma-separated field list")
                        .build())
                .build();

        McpSchema.JsonSchema schema = builder.buildInputSchema(op);

        assertEquals("object", schema.type());
        assertEquals(2, schema.properties().size());

        @SuppressWarnings("unchecked")
        Map<String, Object> userIdProp = (Map<String, Object>) schema.properties().get("userId");
        assertEquals("integer", userIdProp.get("type"));
        assertEquals("User identifier", userIdProp.get("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldsProp = (Map<String, Object>) schema.properties().get("fields");
        assertEquals("string", fieldsProp.get("type"));

        assertEquals(List.of("userId"), schema.required());
    }

    @Test
    void buildInputSchema_withRequestBody() {
        Map<String, Object> bodySchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "age", Map.of("type", "integer")
                )
        );

        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("createUser")
                .method("POST")
                .path("/users")
                .requestBody(CanonicalRequestBody.builder()
                        .required(true)
                        .contentType("application/json")
                        .schema(bodySchema)
                        .build())
                .build();

        McpSchema.JsonSchema schema = builder.buildInputSchema(op);

        assertEquals("object", schema.type());
        assertTrue(schema.properties().containsKey("requestBody"));
        assertEquals(bodySchema, schema.properties().get("requestBody"));
        assertEquals(List.of("requestBody"), schema.required());
    }

    @Test
    void buildInputSchema_combined_pathQueryAndBody() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("updateOrder")
                .method("PUT")
                .path("/orders/{orderId}")
                .parameter(CanonicalParameter.builder()
                        .name("orderId")
                        .location("path")
                        .required(true)
                        .schema(Map.of("type", "string"))
                        .build())
                .parameter(CanonicalParameter.builder()
                        .name("dryRun")
                        .location("query")
                        .required(false)
                        .schema(Map.of("type", "boolean"))
                        .build())
                .requestBody(CanonicalRequestBody.builder()
                        .required(true)
                        .contentType("application/json")
                        .schema(Map.of("type", "object"))
                        .build())
                .build();

        McpSchema.JsonSchema schema = builder.buildInputSchema(op);

        assertEquals(3, schema.properties().size());
        assertTrue(schema.properties().containsKey("orderId"));
        assertTrue(schema.properties().containsKey("dryRun"));
        assertTrue(schema.properties().containsKey("requestBody"));
        assertEquals(List.of("orderId", "requestBody"), schema.required());
    }

    @Test
    void buildInputSchema_optionalBody_notInRequired() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("search")
                .method("POST")
                .path("/search")
                .requestBody(CanonicalRequestBody.builder()
                        .required(false)
                        .contentType("application/json")
                        .schema(Map.of("type", "object"))
                        .build())
                .build();

        McpSchema.JsonSchema schema = builder.buildInputSchema(op);

        assertTrue(schema.properties().containsKey("requestBody"));
        assertNull(schema.required());
    }
}
