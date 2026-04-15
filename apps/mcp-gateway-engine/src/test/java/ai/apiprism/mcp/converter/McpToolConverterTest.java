package ai.apiprism.mcp.converter;

import ai.apiprism.model.*;
import ai.apiprism.mcp.schema.McpToolSchemaBuilder;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpToolConverterTest {

    private final McpToolConverter converter = new McpToolConverter(new McpToolSchemaBuilder());

    @Test
    void convertOperation_basicGet() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("listUsers")
                .method("GET")
                .path("/users")
                .summary("List all users")
                .build();

        McpSchema.Tool tool = converter.convertOperation(op);

        assertEquals("listUsers", tool.name());
        assertTrue(tool.description().contains("GET /users"));
        assertTrue(tool.description().contains("List all users"));
        assertNotNull(tool.inputSchema());
    }

    @Test
    void convertOperation_descriptionCombination() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("createOrder")
                .method("POST")
                .path("/orders")
                .summary("Create a new order")
                .description("Creates an order with the given items and shipping info.")
                .build();

        McpSchema.Tool tool = converter.convertOperation(op);

        assertTrue(tool.description().contains("POST /orders"));
        assertTrue(tool.description().contains("Create a new order"));
        assertTrue(tool.description().contains("Creates an order"));
    }

    @Test
    void convertOperation_noSummaryNoDescription() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("ping")
                .method("GET")
                .path("/ping")
                .build();

        McpSchema.Tool tool = converter.convertOperation(op);

        assertEquals("GET /ping", tool.description());
    }

    @Test
    void convertSnapshot_multipleGroupsAndOperations() {
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("demo").environment("dev").build())
                .title("Demo Service")
                .version("1.0.0")
                .updatedAt(Instant.now())
                .group(CanonicalGroup.builder()
                        .name("Users")
                        .slug("users")
                        .operation(CanonicalOperation.builder()
                                .operationId("listUsers")
                                .method("GET")
                                .path("/users")
                                .summary("List users")
                                .build())
                        .operation(CanonicalOperation.builder()
                                .operationId("getUser")
                                .method("GET")
                                .path("/users/{id}")
                                .parameter(CanonicalParameter.builder()
                                        .name("id")
                                        .location("path")
                                        .required(true)
                                        .schema(Map.of("type", "integer"))
                                        .build())
                                .build())
                        .build())
                .group(CanonicalGroup.builder()
                        .name("Orders")
                        .slug("orders")
                        .operation(CanonicalOperation.builder()
                                .operationId("createOrder")
                                .method("POST")
                                .path("/orders")
                                .requestBody(CanonicalRequestBody.builder()
                                        .required(true)
                                        .contentType("application/json")
                                        .schema(Map.of("type", "object"))
                                        .build())
                                .build())
                        .build())
                .build();

        List<McpSchema.Tool> tools = converter.convertSnapshot(snapshot);

        assertEquals(3, tools.size());
        assertEquals("listUsers", tools.get(0).name());
        assertEquals("getUser", tools.get(1).name());
        assertEquals("createOrder", tools.get(2).name());
    }
}
