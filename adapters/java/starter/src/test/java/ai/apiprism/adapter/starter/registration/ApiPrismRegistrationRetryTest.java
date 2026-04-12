package ai.apiprism.adapter.starter.registration;

import ai.apiprism.adapter.starter.exceptions.ApiPrismRegistrationException;
import ai.apiprism.protocol.registration.ApiRegistrationResponse;
import com.sun.net.httpserver.HttpServer;
import ai.apiprism.protocol.registration.ApiRegistrationRequest;
import ai.apiprism.protocol.registration.ServiceDescriptor;
import ai.apiprism.protocol.registration.SpecPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 RetryTemplate 与 ApiPrismRegistrationClient 配合的重试行为。
 */
class ApiPrismRegistrationRetryTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retriesOnServerErrorThenSucceeds() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/registrations", exchange -> {
            int count = requestCount.incrementAndGet();
            if (count < 3) {
                // 前 2 次返回 503
                byte[] body = "unavailable".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(503, body.length);
                exchange.getResponseBody().write(body);
            } else {
                // 第 3 次返回 200
                byte[] body = """
                        {"accepted":true,"registrationId":"reg-1","message":"ok","warnings":[]}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        RetryTemplate retryTemplate = buildRetryTemplate(5);
        ApiPrismRegistrationClient client = new ApiPrismRegistrationClient(RestClient.builder().build());
        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

        ApiRegistrationResponse response = retryTemplate.execute(
                context -> client.register(baseUrl, sampleRequest()));

        assertEquals(3, requestCount.get());
        assertTrue(response.isAccepted());
    }

    @Test
    void stopsRetryOnNonRetryableClientError() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/registrations", exchange -> {
            requestCount.incrementAndGet();
            byte[] body = "bad request".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        RetryTemplate retryTemplate = buildRetryTemplate(5);
        ApiPrismRegistrationClient client = new ApiPrismRegistrationClient(RestClient.builder().build());
        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

        ApiPrismRegistrationException exception = assertThrows(
                ApiPrismRegistrationException.class,
                () -> retryTemplate.execute(context -> client.register(baseUrl, sampleRequest()))
        );

        // 4xx 不可重试，应只请求 1 次
        assertEquals(1, requestCount.get());
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    void exhaustsRetriesOnPersistentServerError() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/registrations", exchange -> {
            requestCount.incrementAndGet();
            byte[] body = "error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        int maxAttempts = 3;
        RetryTemplate retryTemplate = buildRetryTemplate(maxAttempts);
        ApiPrismRegistrationClient client = new ApiPrismRegistrationClient(RestClient.builder().build());
        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

        assertThrows(
                ApiPrismRegistrationException.class,
                () -> retryTemplate.execute(context -> client.register(baseUrl, sampleRequest()))
        );

        // 应尝试 maxAttempts 次后放弃
        assertEquals(maxAttempts, requestCount.get());
    }

    /**
     * 构建与 {@code ApiPrismAutoConfiguration} 一致的 RetryTemplate，
     * 使用固定 10ms 退避以加速测试。
     */
    private RetryTemplate buildRetryTemplate(int maxAttempts) {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                maxAttempts,
                Map.of(ApiPrismRegistrationException.class, true)
        ) {
            @Override
            public boolean canRetry(RetryContext context) {
                Throwable lastException = context.getLastThrowable();
                if (lastException instanceof ApiPrismRegistrationException ex && !ex.isRetryable()) {
                    return false;
                }
                return super.canRetry(context);
            }
        };

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(10L);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    private ApiRegistrationRequest sampleRequest() {
        return ApiRegistrationRequest.builder()
                .service(ServiceDescriptor.builder()
                        .name("retry-test-service")
                        .environment("test")
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
