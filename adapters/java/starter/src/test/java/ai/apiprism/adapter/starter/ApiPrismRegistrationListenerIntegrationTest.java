package ai.apiprism.adapter.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class ApiPrismRegistrationListenerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RegistrationCaptureServer registrationServer;

    @BeforeEach
    void setUp() throws IOException {
        registrationServer = new RegistrationCaptureServer();
        registrationServer.start();
    }

    @AfterEach
    void tearDown() {
        if (registrationServer != null) {
            registrationServer.close();
        }
    }

    @Test
    void registersOpenApiOnStartupWithFallbackMetadata() throws Exception {
        try (ConfigurableApplicationContext ignored = runApplication(
                SampleApplication.class,
                "server.port=0",
                "spring.application.name=sample-service",
                "spring.profiles.active=qa",
                "apiprism.center-url=" + registrationServer.baseUrl(),
                "apiprism.server-urls[0]=https://api.example.com"
        )) {
            assertTrue(registrationServer.awaitRegistration(Duration.ofSeconds(10)));

            JsonNode request = parseRequest();
            assertEquals("sample-service", request.at("/service/name").asText());
            assertEquals("qa", request.at("/service/environment").asText());
            assertEquals("spring-boot-starter", request.at("/service/adapterType").asText());
            assertEquals("https://api.example.com", request.at("/service/serverUrls/0").asText());
            assertEquals("openapi-json", request.at("/spec/format").asText());
            assertTrue(request.at("/spec/content").asText().contains("/api/hello"));
            assertTrue(request.at("/spec/content").asText().contains("/api/orders/{id}"));
            assertEquals("springdoc-bean", request.at("/extensions/ai.apiprism~1java/openapiSource").asText());
            assertEquals(2, request.at("/extensions/ai.apiprism~1java/mappingCount").asInt());
            assertEquals(2, request.at("/extensions/ai.apiprism~1java/documentedOperationCount").asInt());
            assertTrue(request.at("/extensions/ai.apiprism~1java/undocumentedMappings").isMissingNode());
        }
    }

    @Test
    void skipsRegistrationWhenRegisterOnStartupDisabled() {
        try (ConfigurableApplicationContext ignored = runApplication(
                SampleApplication.class,
                "server.port=0",
                "apiprism.center-url=" + registrationServer.baseUrl(),
                "apiprism.project-name=disabled-service",
                "apiprism.register-on-startup=false"
        )) {
            assertFalse(registrationServer.awaitRegistration(Duration.ofSeconds(2)));
            assertEquals(0, registrationServer.requestCount());
        }
    }

    @Test
    void warnsWhenOpenApiMissesMappedOperations(CapturedOutput output) throws Exception {
        try (ConfigurableApplicationContext ignored = runApplication(
                UndocumentedEndpointApplication.class,
                "server.port=0",
                "apiprism.center-url=" + registrationServer.baseUrl(),
                "apiprism.project-name=undocumented-service"
        )) {
            assertTrue(registrationServer.awaitRegistration(Duration.ofSeconds(10)));

            JsonNode request = parseRequest();
            JsonNode undocumentedMappings = request.at("/extensions/ai.apiprism~1java/undocumentedMappings");
            assertEquals(1, undocumentedMappings.at("/total").asInt());
            assertEquals("GET /api/missing", undocumentedMappings.at("/sample/0").asText());
            assertTrue(output.getOut().contains("OpenAPI document is missing 1 mapped operations"));
        }
    }

    private JsonNode parseRequest() throws IOException {
        String requestBody = registrationServer.requestBody();
        assertNotNull(requestBody);
        return objectMapper.readTree(requestBody);
    }

    private ConfigurableApplicationContext runApplication(Class<?> applicationClass, String... properties) {
        SpringApplication application = new SpringApplication(applicationClass);
        application.setDefaultProperties(java.util.Map.of("spring.main.banner-mode", "off"));
        String[] args = Arrays.stream(properties)
                .map(property -> property.startsWith("--") ? property : "--" + property)
                .toArray(String[]::new);
        return application.run(args);
    }

    @SpringBootApplication
    static class SampleApplication {

        @RestController
        static class SampleController {

            @GetMapping("/api/hello")
            String hello() {
                return "hello";
            }

            @PostMapping("/api/orders/{id}")
            String createOrder(@PathVariable String id) {
                return id;
            }
        }
    }

    @SpringBootApplication
    static class UndocumentedEndpointApplication {

        @RestController
        static class UndocumentedController {

            @GetMapping("/api/documented")
            String documented() {
                return "documented";
            }

            @GetMapping("/api/missing")
            String missing() {
                return "missing";
            }
        }

        @Bean
        OpenApiCustomizer removeEndpointFromOpenApi() {
            return openApi -> {
                if (openApi.getPaths() != null) {
                    openApi.getPaths().remove("/api/missing");
                }
            };
        }
    }

    private static final class RegistrationCaptureServer implements AutoCloseable {

        private HttpServer server;
        private final AtomicReference<String> requestBody = new AtomicReference<>();
        private final AtomicInteger requestCount = new AtomicInteger();
        private final CountDownLatch registrationLatch = new CountDownLatch(1);

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/registrations", exchange -> {
                requestCount.incrementAndGet();
                requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

                byte[] responseBody = """
                        {"accepted":true,"registrationId":"reg-1","message":"ok","warnings":[]}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBody.length);
                exchange.getResponseBody().write(responseBody);
                exchange.close();
                registrationLatch.countDown();
            });
            server.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        boolean awaitRegistration(Duration timeout) {
            try {
                return registrationLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        String requestBody() {
            return requestBody.get();
        }

        int requestCount() {
            return requestCount.get();
        }

        @Override
        public void close() {
            if (server != null) {
                server.stop(0);
            }
        }
    }
}
