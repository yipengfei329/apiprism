package ai.apiprism.adapter.starter.registration;

import com.sun.net.httpserver.HttpServer;
import ai.apiprism.adapter.starter.exceptions.ApiPrismRegistrationException;
import ai.apiprism.protocol.registration.ApiRegistrationRequest;
import ai.apiprism.protocol.registration.ApiRegistrationResponse;
import ai.apiprism.protocol.registration.ServiceDescriptor;
import ai.apiprism.protocol.registration.SpecPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiPrismRegistrationClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void registersThroughRestClient() throws IOException {
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/registrations", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] responseBody = """
                    {"accepted":true,"registrationId":"reg-1","message":"ok","warnings":[]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();

        ApiPrismRegistrationClient client = new ApiPrismRegistrationClient(RestClient.builder().build());
        ApiRegistrationResponse response = client.register(baseUrl(), sampleRequest());

        assertEquals("POST", requestMethod.get());
        assertEquals("/api/v1/registrations", requestPath.get());
        assertTrue(requestBody.get().contains("\"name\":\"order-service\""));
        assertTrue(requestBody.get().contains("\"openapi-json\""));
        assertTrue(response.isAccepted());
        assertEquals("reg-1", response.getRegistrationId());
    }

    @Test
    void throwsRetryableExceptionFor5xxResponse() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/registrations", exchange -> {
            byte[] responseBody = "internal error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();

        ApiPrismRegistrationClient client = new ApiPrismRegistrationClient(RestClient.builder().build());

        ApiPrismRegistrationException exception = assertThrows(
                ApiPrismRegistrationException.class,
                () -> client.register(baseUrl(), sampleRequest())
        );
        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("internal error"));
        assertTrue(exception.isRetryable());
    }

    @Test
    void throwsNonRetryableExceptionFor4xxResponse() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/registrations", exchange -> {
            byte[] responseBody = "bad request".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();

        ApiPrismRegistrationClient client = new ApiPrismRegistrationClient(RestClient.builder().build());

        ApiPrismRegistrationException exception = assertThrows(
                ApiPrismRegistrationException.class,
                () -> client.register(baseUrl(), sampleRequest())
        );
        assertTrue(exception.getMessage().contains("400"));
        assertFalse(exception.isRetryable());
    }

    @Test
    void throwsRetryableExceptionForConnectionError() {
        ApiPrismRegistrationClient client = new ApiPrismRegistrationClient(RestClient.builder().build());

        // 连接一个不存在的端口，触发连接异常
        ApiPrismRegistrationException exception = assertThrows(
                ApiPrismRegistrationException.class,
                () -> client.register(URI.create("http://127.0.0.1:1"), sampleRequest())
        );
        assertTrue(exception.isRetryable());
    }

    private URI baseUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private ApiRegistrationRequest sampleRequest() {
        return ApiRegistrationRequest.builder()
                .service(ServiceDescriptor.builder()
                        .name("order-service")
                        .environment("dev")
                        .serverUrl("https://api.example.com")
                        .adapterType("spring-boot-starter")
                        .build())
                .spec(SpecPayload.builder()
                        .format("openapi-json")
                        .content("{\"openapi\":\"3.0.1\"}")
                        .build())
                .build();
    }
}
