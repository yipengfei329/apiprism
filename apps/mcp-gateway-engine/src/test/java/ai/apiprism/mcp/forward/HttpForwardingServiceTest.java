package ai.apiprism.mcp.forward;

import ai.apiprism.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpForwardingServiceTest {

    private final HttpForwardingService service =
            new HttpForwardingService(RestClient.create(), new ObjectMapper());

    @Test
    void buildRequestUrl_pathTemplateSubstitution() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("getUser")
                .method("GET")
                .path("/users/{userId}/orders/{orderId}")
                .parameter(CanonicalParameter.builder()
                        .name("userId").location("path").required(true)
                        .schema(Map.of("type", "integer")).build())
                .parameter(CanonicalParameter.builder()
                        .name("orderId").location("path").required(true)
                        .schema(Map.of("type", "string")).build())
                .build();

        Map<String, Object> args = Map.of("userId", 42, "orderId", "abc-123");

        String url = service.buildRequestUrl("http://localhost:8081", op, args);

        assertEquals("http://localhost:8081/users/42/orders/abc-123", url);
    }

    @Test
    void buildRequestUrl_queryParams() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("listUsers")
                .method("GET")
                .path("/users")
                .parameter(CanonicalParameter.builder()
                        .name("page").location("query").required(false)
                        .schema(Map.of("type", "integer")).build())
                .parameter(CanonicalParameter.builder()
                        .name("size").location("query").required(false)
                        .schema(Map.of("type", "integer")).build())
                .build();

        Map<String, Object> args = Map.of("page", 1, "size", 20);

        String url = service.buildRequestUrl("http://localhost:8081", op, args);

        assertTrue(url.startsWith("http://localhost:8081/users?"));
        assertTrue(url.contains("page=1"));
        assertTrue(url.contains("size=20"));
    }

    @Test
    void buildRequestUrl_mixedPathAndQuery() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("getUserOrders")
                .method("GET")
                .path("/users/{id}/orders")
                .parameter(CanonicalParameter.builder()
                        .name("id").location("path").required(true)
                        .schema(Map.of("type", "integer")).build())
                .parameter(CanonicalParameter.builder()
                        .name("status").location("query").required(false)
                        .schema(Map.of("type", "string")).build())
                .build();

        Map<String, Object> args = Map.of("id", 5, "status", "pending");

        String url = service.buildRequestUrl("http://localhost:8081", op, args);

        assertEquals("http://localhost:8081/users/5/orders?status=pending", url);
    }

    @Test
    void buildRequestUrl_noParams() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("health")
                .method("GET")
                .path("/health")
                .build();

        String url = service.buildRequestUrl("http://localhost:8081", op, Map.of());

        assertEquals("http://localhost:8081/health", url);
    }

    @Test
    void buildRequestUrl_encodesSpecialCharacters() {
        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("search")
                .method("GET")
                .path("/search")
                .parameter(CanonicalParameter.builder()
                        .name("q").location("query").required(true)
                        .schema(Map.of("type", "string")).build())
                .build();

        Map<String, Object> args = Map.of("q", "hello world&more");

        String url = service.buildRequestUrl("http://localhost:8081", op, args);

        assertTrue(url.contains("q=hello+world%26more") || url.contains("q=hello%20world%26more"));
    }

    @Test
    void forward_noServerUrl_returnsError() {
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name("test").environment("dev").build())
                .title("Test").version("1.0").updatedAt(Instant.now())
                .build();

        CanonicalOperation op = CanonicalOperation.builder()
                .operationId("test").method("GET").path("/test").build();

        var result = service.forward(snapshot, op, Map.of());

        assertTrue(result.isError());
        assertTrue(result.content().get(0) instanceof McpSchema.TextContent);
        assertTrue(((McpSchema.TextContent) result.content().get(0)).text().contains("No server URL"));
    }
}
