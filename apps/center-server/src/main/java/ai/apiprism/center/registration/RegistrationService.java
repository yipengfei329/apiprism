package ai.apiprism.center.registration;

import ai.apiprism.center.repository.RegistrationRepository;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.protocol.registration.ApiRegistrationRequest;
import ai.apiprism.protocol.registration.ApiRegistrationResponse;
import ai.apiprism.openapi.NormalizationResult;
import ai.apiprism.openapi.OpenApiNormalizer;
import ai.apiprism.mcp.event.ServiceRegisteredEvent;
import io.hypersistence.tsid.TSID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 注册服务：负责接收注册请求、调用规范化流水线、持久化注册快照。
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final RegistrationRepository repository;
    private final OpenApiNormalizer normalizer;
    private final ApplicationEventPublisher eventPublisher;

    public RegistrationService(RegistrationRepository repository, OpenApiNormalizer normalizer,
                               ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 接收注册请求，规范化 OpenAPI 规范并保存到注册库。
     * 若规范无法解析，{@link ai.apiprism.openapi.exceptions.NormalizationException} 向上抛出，由错误处理器转换为 422。
     */
    public ApiRegistrationResponse register(ApiRegistrationRequest request) {
        String serviceName = request.getService().getName();
        String environment = request.getService().getEnvironment();

        log.info("Registering service {} ({}) via adapter {}",
                serviceName, environment, request.getService().getAdapterType());

        NormalizationResult result = normalizer.normalize(
                serviceName,
                environment,
                request.getService().getTitle(),
                request.getService().getVersion(),
                request.getService().getServerUrls(),
                request.getSpec().getContent()
        );

        if (!result.getWarnings().isEmpty()) {
            log.warn("Normalization produced {} warning(s) for service {} ({}): {}",
                    result.getWarnings().size(), serviceName, environment, result.getWarnings());
        }

        String incomingId = TSID.Factory.getTsid().toString();
        // 使用语义 hash：仅对规范化后的接口定义内容（路径、参数、结构等）做哈希，忽略格式空白
        String specHash = SemanticHasher.hash(result.getSnapshot());
        StoredRegistration saved = repository.saveRevision(StoredRegistration.builder()
                .id(incomingId)
                .rawSpec(request.getSpec().getContent())
                .specFormat(request.getSpec().getFormat())
                .adapterType(request.getService().getAdapterType())
                .specHash(specHash)
                .snapshot(result.getSnapshot())
                .warnings(result.getWarnings())
                .extensions(request.getExtensions())
                .build());

        log.info("Registered service {} ({}) with revision id {} seq {} ({} group(s), {} warning(s))",
                serviceName, environment, saved.getId(), saved.getRevisionSeq(),
                result.getSnapshot().getGroups().size(), result.getWarnings().size());

        // 通知 MCP 网关引擎刷新工具定义
        eventPublisher.publishEvent(new ServiceRegisteredEvent(this, serviceName, environment));

        return ApiRegistrationResponse.builder()
                .accepted(true)
                .registrationId(saved.getId())
                .message("Registration accepted")
                .warnings(result.getWarnings())
                .build();
    }

}
