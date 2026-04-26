package ai.apiprism.center.markdown;

import ai.apiprism.center.catalog.ServiceCatalogItem;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentMarkdownRendererTest {

    private final AgentMarkdownRenderer renderer = new AgentMarkdownRenderer();

    @Test
    void rendersFullOperationWithAllSections() {
        Map<String, Object> bodySchema = new LinkedHashMap<>();
        bodySchema.put("type", "object");
        Map<String, Object> bodyProps = new LinkedHashMap<>();
        bodyProps.put("name", Map.of("type", "string"));
        bodyProps.put("quantity", Map.of("type", "integer"));
        bodySchema.put("properties", bodyProps);
        bodySchema.put("required", List.of("name"));

        Map<String, Object> responseSchema = new LinkedHashMap<>();
        responseSchema.put("type", "object");
        Map<String, Object> responseProps = new LinkedHashMap<>();
        responseProps.put("orderId", Map.of("type", "string"));
        responseProps.put("status", Map.of("type", "string"));
        responseSchema.put("properties", responseProps);

        CanonicalOperation operation = CanonicalOperation.builder()
                .operationId("createOrder")
                .method("POST")
                .path("/orders")
                .summary("Create a new order")
                .description("Creates an order with the given details.")
                .tag("orders")
                .securityRequirement("bearerAuth")
                .parameter(CanonicalParameter.builder()
                        .name("X-Request-Id")
                        .location("header")
                        .required(false)
                        .schema(Map.of("type", "string"))
                        .description("Trace ID")
                        .build())
                .requestBody(CanonicalRequestBody.builder()
                        .required(true)
                        .contentType("application/json")
                        .schema(bodySchema)
                        .build())
                .response(CanonicalResponse.builder()
                        .statusCode("201")
                        .description("Created")
                        .contentType("application/json")
                        .schema(responseSchema)
                        .build())
                .response(CanonicalResponse.builder()
                        .statusCode("400")
                        .description("Bad Request")
                        .build())
                .build();

        CanonicalServiceSnapshot snapshot = buildSnapshot(operation, "http://localhost:8081");

        String md = renderer.renderAgentOperation(snapshot, operation);

        // 标题和摘要
        assertTrue(md.contains("# createOrder"));
        assertTrue(md.contains("> Create a new order"));
        assertTrue(md.contains("Creates an order with the given details."));

        // Endpoint
        assertTrue(md.contains("`POST /orders`"));
        assertTrue(md.contains("Base URL: `http://localhost:8081`"));

        // Authentication
        assertTrue(md.contains("## Authentication"));
        assertTrue(md.contains("`bearerAuth`"));

        // Input Parameters — JSON Schema 代码块
        assertTrue(md.contains("## Input Parameters"));
        assertTrue(md.contains("\"in\" : \"header\""));

        // Request Body
        assertTrue(md.contains("## Request Body"));
        assertTrue(md.contains("Content-Type: `application/json`"));
        assertTrue(md.contains("\"name\""));
        assertTrue(md.contains("\"quantity\""));

        // Output
        assertTrue(md.contains("## Output"));
        assertTrue(md.contains("### 201 Created (`application/json`)"));
        assertTrue(md.contains("\"orderId\""));
        assertTrue(md.contains("### 400 Bad Request"));

        // Curl example
        assertTrue(md.contains("## Example"));
        assertTrue(md.contains("curl -X POST"));
        assertTrue(md.contains("Authorization: Bearer <TOKEN>"));
        assertTrue(md.contains("-d '"));
    }

    @Test
    void rendersMinimalOperationWithoutOptionalSections() {
        CanonicalOperation operation = CanonicalOperation.builder()
                .operationId("healthCheck")
                .method("GET")
                .path("/health")
                .build();

        CanonicalServiceSnapshot snapshot = buildSnapshot(operation, null);

        String md = renderer.renderAgentOperation(snapshot, operation);

        assertTrue(md.contains("# healthCheck"));
        assertTrue(md.contains("`GET /health`"));
        // 无 serverUrl 时使用占位符
        assertTrue(md.contains("https://<SERVICE_HOST>"));
        // 无参数、body、认证时不输出对应 section
        assertFalse(md.contains("## Authentication"));
        assertFalse(md.contains("## Input Parameters"));
        assertFalse(md.contains("## Request Body"));
    }

    @Test
    void curlIncludesQueryParameters() {
        CanonicalOperation operation = CanonicalOperation.builder()
                .operationId("listOrders")
                .method("GET")
                .path("/orders")
                .parameter(CanonicalParameter.builder()
                        .name("page")
                        .location("query")
                        .required(false)
                        .schema(Map.of("type", "integer"))
                        .description("Page number")
                        .build())
                .parameter(CanonicalParameter.builder()
                        .name("size")
                        .location("query")
                        .required(false)
                        .schema(Map.of("type", "integer"))
                        .description("Page size")
                        .build())
                .build();

        CanonicalServiceSnapshot snapshot = buildSnapshot(operation, "https://api.example.com");
        String md = renderer.renderAgentOperation(snapshot, operation);

        assertTrue(md.contains("page=<page>"));
        assertTrue(md.contains("size=<size>"));
        // curl 不应包含 -d
        assertFalse(md.contains("-d '"));
    }

    @Test
    void rendersServiceIndexWithMultipleGroups() {
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("order-service").environment("prod").build())
                .title("Order Service")
                .version("2.1.0")
                .group(CanonicalGroup.builder()
                        .name("orders").slug("orders")
                        .operation(CanonicalOperation.builder()
                                .operationId("createOrder").method("POST").path("/orders").summary("Create a new order").build())
                        .operation(CanonicalOperation.builder()
                                .operationId("getOrder").method("GET").path("/orders/{id}").summary("Get order by ID").build())
                        .build())
                .group(CanonicalGroup.builder()
                        .name("payments").slug("payments")
                        .operation(CanonicalOperation.builder()
                                .operationId("createPayment").method("POST").path("/payments").summary("Process a payment").build())
                        .build())
                .updatedAt(Instant.now())
                .build();

        String md = renderer.renderServiceIndex(snapshot, "http://localhost:8080");

        // 标题和元信息
        assertTrue(md.contains("# Order Service API Index"));
        assertTrue(md.contains("order-service"));
        assertTrue(md.contains("prod"));
        assertTrue(md.contains("2.1.0"));

        // 统计
        assertTrue(md.contains("**3 operations**"));
        assertTrue(md.contains("**2 groups**"));

        // group heading
        assertTrue(md.contains("## orders"));
        assertTrue(md.contains("## payments"));

        // 表格行和相对链接（URL 含 group slug 段，与前端路由 [service]/[environment]/[group]/[operationId] 对齐）
        assertTrue(md.contains("[createOrder](http://localhost:8080/order-service/prod/orders/createOrder/apidocs.md)"));
        assertTrue(md.contains("[getOrder](http://localhost:8080/order-service/prod/orders/getOrder/apidocs.md)"));
        assertTrue(md.contains("[createPayment](http://localhost:8080/order-service/prod/payments/createPayment/apidocs.md)"));

        // 方法和路径
        assertTrue(md.contains("| POST | `/orders`"));
        assertTrue(md.contains("| GET | `/orders/{id}`"));
    }

    @Test
    void rendersServiceIndexWithEmptyGroup() {
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("svc").environment("dev").build())
                .title("Empty Service")
                .version("1.0.0")
                .group(CanonicalGroup.builder().name("empty").slug("empty").build())
                .updatedAt(Instant.now())
                .build();

        String md = renderer.renderServiceIndex(snapshot, "http://localhost:8080");

        assertTrue(md.contains("## empty"));
        assertTrue(md.contains("**0 operations**"));
        // 不应包含表格头（无 operation 则无表格）
        assertFalse(md.contains("| Operation |"));
    }

    @Test
    void rendersGlobalCatalogWithMultipleServices() {
        List<ServiceCatalogItem> services = List.of(
                ServiceCatalogItem.builder()
                        .name("order-service").environment("prod").title("Order Service").version("2.1.0")
                        .updatedAt(Instant.now())
                        .groups(List.of(
                                ServiceCatalogItem.GroupRef.builder().name("orders").slug("orders").build(),
                                ServiceCatalogItem.GroupRef.builder().name("payments").slug("payments").build()))
                        .build(),
                ServiceCatalogItem.builder()
                        .name("user-service").environment("prod").title("User Service").version("1.5.0")
                        .updatedAt(Instant.now())
                        .groups(List.of(
                                ServiceCatalogItem.GroupRef.builder().name("users").slug("users").build()))
                        .build()
        );

        String md = renderer.renderGlobalCatalog(services, "http://localhost:8080");

        assertTrue(md.contains("# API Catalog"));
        assertTrue(md.contains("2 registered services"));
        assertTrue(md.contains("[View](http://localhost:8080/order-service/prod/apidocs.md)"));
        assertTrue(md.contains("[View](http://localhost:8080/user-service/prod/apidocs.md)"));
        assertTrue(md.contains("orders, payments"));
    }

    @Test
    void rendersGlobalCatalogEmpty() {
        String md = renderer.renderGlobalCatalog(List.of(), "http://localhost:8080");

        assertTrue(md.contains("# API Catalog"));
        assertTrue(md.contains("0 registered services"));
    }

    @Test
    void escapesPipeInSummary() {
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("svc").environment("dev").build())
                .title("Test")
                .version("1.0.0")
                .group(CanonicalGroup.builder()
                        .name("test").slug("test")
                        .operation(CanonicalOperation.builder()
                                .operationId("pipeOp").method("GET").path("/test")
                                .summary("A | B choice").build())
                        .build())
                .updatedAt(Instant.now())
                .build();

        String md = renderer.renderServiceIndex(snapshot, "http://localhost:8080");

        // pipe 应被转义，不会破坏 markdown 表格
        assertTrue(md.contains("A \\| B choice"));
    }

    private CanonicalServiceSnapshot buildSnapshot(CanonicalOperation operation, String serverUrl) {
        var builder = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder()
                        .name("test-service")
                        .environment("dev")
                        .build())
                .title("Test Service")
                .version("1.0.0")
                .group(CanonicalGroup.builder()
                        .name("default")
                        .operation(operation)
                        .build())
                .updatedAt(Instant.now());
        if (serverUrl != null) {
            builder.serverUrl(serverUrl);
        }
        return builder.build();
    }
}
