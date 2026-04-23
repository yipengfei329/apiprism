package ai.apiprism.center.markdown;

import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void rendersServiceAndOperationMarkdown() {
        CanonicalOperation operation = CanonicalOperation.builder()
                .operationId("getOrder")
                .method("GET")
                .path("/orders/{id}")
                .summary("Fetch order")
                .description("Returns an order by id")
                .tag("orders")
                .securityRequirement("bearerAuth")
                .parameter(CanonicalParameter.builder()
                        .name("id")
                        .location("path")
                        .required(true)
                        .schema(Map.of("type", "string"))
                        .description("Order id")
                        .build())
                .response(CanonicalResponse.builder()
                        .statusCode("200")
                        .description("OK")
                        .contentType("application/json")
                        .schema(Map.of("type", "object"))
                        .build())
                .build();
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder()
                        .name("order-service")
                        .environment("dev")
                        .build())
                .title("Order Service")
                .version("1.0.0")
                .serverUrl("http://localhost:8081")
                .group(CanonicalGroup.builder()
                        .name("orders")
                        .description("Order operations")
                        .operation(operation)
                        .build())
                .updatedAt(Instant.now())
                .build();

        assertTrue(renderer.renderService(snapshot).contains("## Groups"));
        assertTrue(renderer.renderOperation(snapshot, operation).contains("`GET`"));
    }

    @Test
    void rendersNestedSchemaPropertiesWithDotNotation() {
        Map<String, Object> addressSchema = new LinkedHashMap<>();
        addressSchema.put("type", "object");
        Map<String, Object> addressProps = new LinkedHashMap<>();
        addressProps.put("street", Map.of("type", "string", "description", "街道"));
        addressProps.put("city", Map.of("type", "string", "description", "城市"));
        addressSchema.put("properties", addressProps);

        Map<String, Object> responseSchema = new LinkedHashMap<>();
        responseSchema.put("type", "object");
        Map<String, Object> topProps = new LinkedHashMap<>();
        topProps.put("orderId", Map.of("type", "string", "description", "订单号"));
        topProps.put("address", addressSchema);
        responseSchema.put("properties", topProps);
        responseSchema.put("required", List.of("orderId"));

        CanonicalOperation operation = CanonicalOperation.builder()
                .operationId("getOrder")
                .method("GET")
                .path("/orders/{id}")
                .response(CanonicalResponse.builder()
                        .statusCode("200")
                        .description("OK")
                        .contentType("application/json")
                        .schema(responseSchema)
                        .build())
                .build();
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("svc").environment("dev").build())
                .title("Test")
                .version("1.0")
                .group(CanonicalGroup.builder().name("g").operation(operation).build())
                .updatedAt(Instant.now())
                .build();

        String md = renderer.renderOperation(snapshot, operation);
        assertTrue(md.contains("orderId"), "应包含 orderId 字段");
        assertTrue(md.contains("address.street"), "嵌套字段应使用 address.street 点号路径");
        assertTrue(md.contains("address.city"), "嵌套字段应使用 address.city 点号路径");
    }

    @Test
    void rendersArrayItemPropertiesInMarkdown() {
        Map<String, Object> itemSchema = new LinkedHashMap<>();
        itemSchema.put("type", "object");
        Map<String, Object> itemProps = new LinkedHashMap<>();
        itemProps.put("productId", Map.of("type", "string", "description", "商品ID"));
        itemProps.put("quantity", Map.of("type", "integer", "description", "数量"));
        itemSchema.put("properties", itemProps);

        Map<String, Object> arraySchema = new LinkedHashMap<>();
        arraySchema.put("type", "array");
        arraySchema.put("items", itemSchema);

        CanonicalOperation operation = CanonicalOperation.builder()
                .operationId("listItems")
                .method("GET")
                .path("/items")
                .response(CanonicalResponse.builder()
                        .statusCode("200")
                        .description("OK")
                        .contentType("application/json")
                        .schema(arraySchema)
                        .build())
                .build();
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("svc").environment("dev").build())
                .title("Test")
                .version("1.0")
                .group(CanonicalGroup.builder().name("g").operation(operation).build())
                .updatedAt(Instant.now())
                .build();

        String md = renderer.renderOperation(snapshot, operation);
        assertTrue(md.contains("productId"), "array items 的字段应被渲染");
        assertTrue(md.contains("quantity"), "array items 的字段应被渲染");
    }
}
