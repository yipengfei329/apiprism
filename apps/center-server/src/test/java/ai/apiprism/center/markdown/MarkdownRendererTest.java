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
}
