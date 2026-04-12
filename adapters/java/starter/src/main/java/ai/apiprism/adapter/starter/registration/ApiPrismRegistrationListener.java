package ai.apiprism.adapter.starter.registration;

import ai.apiprism.adapter.starter.ApiPrismProperties;
import ai.apiprism.adapter.starter.exceptions.ApiPrismRegistrationException;
import ai.apiprism.adapter.starter.inspection.ApiPrismMappingInspector;
import ai.apiprism.adapter.starter.inspection.ApiPrismRegistrationDiagnostics;
import ai.apiprism.adapter.starter.openapi.ApiPrismOpenApiDocument;
import ai.apiprism.adapter.starter.openapi.ApiPrismOpenApiSupplier;
import ai.apiprism.protocol.registration.ApiRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

/**
 * 监听 {@link ApplicationReadyEvent}，在应用就绪后触发 APIPrism 注册流程。
 * <p>
 * 注册在独立的 daemon 线程中异步执行，不阻塞应用启动。
 * HTTP 调用通过 {@link RetryTemplate} 实现指数退避重试，
 * 仅对可重试的瞬态故障（5xx、网络超时）自动重试。
 * <p>
 * 本类只负责协调流程，具体职责分别委托给：
 * <ul>
 *   <li>{@link ServiceMetadataResolver}：解析服务元数据</li>
 *   <li>{@link ApiPrismOpenApiSupplier}：获取 OpenAPI 文档</li>
 *   <li>{@link ApiPrismMappingInspector}：映射检查与诊断</li>
 *   <li>{@link RegistrationRequestFactory}：构建注册请求</li>
 *   <li>{@link ApiPrismRegistrationClient}：发送注册请求</li>
 * </ul>
 */
public class ApiPrismRegistrationListener
        implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ApiPrismRegistrationListener.class);

    private final ApiPrismProperties properties;
    private final ApiPrismRegistrationClient registrationClient;
    private final ApiPrismOpenApiSupplier openApiSupplier;
    private final ApiPrismMappingInspector mappingInspector;
    private final ServiceMetadataResolver metadataResolver;
    private final RegistrationRequestFactory requestFactory;
    private final RetryTemplate retryTemplate;

    private volatile boolean shuttingDown = false;
    private volatile Thread registrationThread;

    public ApiPrismRegistrationListener(
            ApiPrismProperties properties,
            ApiPrismRegistrationClient registrationClient,
            ApiPrismOpenApiSupplier openApiSupplier,
            ApiPrismMappingInspector mappingInspector,
            ServiceMetadataResolver metadataResolver,
            RegistrationRequestFactory requestFactory,
            RetryTemplate retryTemplate
    ) {
        this.properties = properties;
        this.registrationClient = registrationClient;
        this.openApiSupplier = openApiSupplier;
        this.mappingInspector = mappingInspector;
        this.metadataResolver = metadataResolver;
        this.requestFactory = requestFactory;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!properties.isRegisterOnStartup()) {
            return;
        }
        if (!(event.getApplicationContext() instanceof WebServerApplicationContext webCtx)) {
            log.debug("Skipping APIPrism registration because the application is not a web application.");
            return;
        }

        String localBaseUrl = "http://127.0.0.1:" + webCtx.getWebServer().getPort();
        ServiceMetadata metadata = metadataResolver.resolve(localBaseUrl);

        registrationThread = new Thread(
                () -> performRegistration(metadata, localBaseUrl),
                "apiprism-registration"
        );
        registrationThread.setDaemon(true);
        registrationThread.start();
    }

    private void performRegistration(ServiceMetadata metadata, String localBaseUrl) {
        try {
            log.info("Starting APIPrism registration for service {} ({}) using OpenAPI path {}",
                    metadata.projectName(), metadata.environment(), properties.getOpenapiPath());

            ApiPrismOpenApiDocument document = openApiSupplier.fetch(localBaseUrl);
            ApiPrismRegistrationDiagnostics diagnostics = mappingInspector.inspect(document.getOpenApi());
            logUndocumentedMappings(metadata, diagnostics);

            ApiRegistrationRequest request = requestFactory.build(metadata, document, diagnostics);

            retryTemplate.execute(context -> {
                if (shuttingDown) {
                    throw new ApiPrismRegistrationException(
                            "Registration cancelled: application is shutting down", false);
                }
                return registrationClient.register(properties.getCenterUrl(), request);
            });

            log.info("Registered service {} ({}) with APIPrism center at {} using {} operations from {}",
                    metadata.projectName(), metadata.environment(),
                    properties.getCenterUrl(), document.getOperationCount(), document.getSource());
        } catch (RuntimeException exception) {
            log.warn("APIPrism registration failed for service {} ({}): {}",
                    metadata.projectName(), metadata.environment(), exception.getMessage());
        }
    }

    @Override
    public void destroy() {
        shuttingDown = true;
        Thread t = registrationThread;
        if (t != null) {
            t.interrupt();
            log.debug("APIPrism registration thread interrupted due to application shutdown.");
        }
    }

    private void logUndocumentedMappings(ServiceMetadata metadata, ApiPrismRegistrationDiagnostics diagnostics) {
        if (!diagnostics.hasUndocumentedMappings()) {
            return;
        }
        List<String> sample = diagnostics.getUndocumentedMappings().stream().limit(5).toList();
        log.warn("OpenAPI document is missing {} mapped operations for service {} ({}). Sample: {}",
                diagnostics.getUndocumentedMappings().size(),
                metadata.projectName(),
                metadata.environment(),
                sample);
    }
}
