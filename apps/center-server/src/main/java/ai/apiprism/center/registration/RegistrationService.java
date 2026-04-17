package ai.apiprism.center.registration;

import ai.apiprism.center.localization.LocalizationEntity;
import ai.apiprism.center.localization.LocalizationField;
import ai.apiprism.center.localization.LocalizationRepository;
import ai.apiprism.center.localization.LocalizationSource;
import ai.apiprism.center.registration.persistence.DeploymentRepository;
import ai.apiprism.center.registration.persistence.EnvironmentRepository;
import ai.apiprism.center.registration.persistence.OperationChildrenRepository;
import ai.apiprism.center.registration.persistence.OperationRepository;
import ai.apiprism.center.registration.persistence.OperationVersionRepository;
import ai.apiprism.center.registration.persistence.PersistenceRows.DeploymentRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.EnvironmentRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationParameterRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationResponseRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.ServiceRow;
import ai.apiprism.center.registration.persistence.ServiceRepository;
import ai.apiprism.center.registration.persistence.TagRepository;
import ai.apiprism.center.registration.persistence.TagRepository.TagSpec;
import ai.apiprism.center.repository.FileSpecStore;
import ai.apiprism.mcp.event.ServiceRegisteredEvent;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.hash.CanonicalHasher;
import ai.apiprism.openapi.NormalizationResult;
import ai.apiprism.openapi.OpenApiNormalizer;
import ai.apiprism.protocol.registration.ApiRegistrationRequest;
import ai.apiprism.protocol.registration.ApiRegistrationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 注册服务：接收注册请求 → 规范化 → 拆表 upsert。
 *
 * <p>hash 分两层：
 * <ul>
 *   <li>部署级 spec_hash：未变直接 touch 时间戳并返回，不解析、不入库。</li>
 *   <li>接口级 definition_hash：只覆盖结构，描述类变化不触发 hash 变化，
 *       也不会在 operation_definition_versions 重复追加。</li>
 * </ul>
 * 描述类内容（title/summary/description）永远覆盖 source='adapter' 行；
 * source='center' 行由中心运营人员手动维护，适配器不会覆盖。
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private static final String DEFAULT_LOCALE_FALLBACK = "zh-CN";

    private final ServiceRepository serviceRepo;
    private final EnvironmentRepository envRepo;
    private final DeploymentRepository deployRepo;
    private final TagRepository tagRepo;
    private final OperationRepository opRepo;
    private final OperationChildrenRepository childrenRepo;
    private final OperationVersionRepository versionRepo;
    private final LocalizationRepository localizationRepo;
    private final OpenApiNormalizer normalizer;
    private final ObjectMapper objectMapper;
    private final FileSpecStore fileSpecStore;
    private final ApplicationEventPublisher eventPublisher;

    public RegistrationService(ServiceRepository serviceRepo,
                               EnvironmentRepository envRepo,
                               DeploymentRepository deployRepo,
                               TagRepository tagRepo,
                               OperationRepository opRepo,
                               OperationChildrenRepository childrenRepo,
                               OperationVersionRepository versionRepo,
                               LocalizationRepository localizationRepo,
                               OpenApiNormalizer normalizer,
                               ObjectMapper objectMapper,
                               FileSpecStore fileSpecStore,
                               ApplicationEventPublisher eventPublisher) {
        this.serviceRepo = serviceRepo;
        this.envRepo = envRepo;
        this.deployRepo = deployRepo;
        this.tagRepo = tagRepo;
        this.opRepo = opRepo;
        this.childrenRepo = childrenRepo;
        this.versionRepo = versionRepo;
        this.localizationRepo = localizationRepo;
        this.normalizer = normalizer;
        this.objectMapper = objectMapper;
        this.fileSpecStore = fileSpecStore;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ApiRegistrationResponse register(ApiRegistrationRequest request) {
        String serviceName = request.getService().getName();
        String environmentCode = request.getService().getEnvironment();
        String adapterType = request.getService().getAdapterType();
        String defaultLocale = resolveDefaultLocale(request);

        log.info("Registering service {} ({}) via adapter {}",
                serviceName, environmentCode, adapterType);

        String rawSpec = request.getSpec().getContent();
        String specHash = sha256(OpenApiNormalizer.VERSION + "\n" + rawSpec);

        ServiceRow service = serviceRepo.upsertByName(serviceName, defaultLocale);
        EnvironmentRow environment = envRepo.upsertByCode(environmentCode);
        Optional<DeploymentRow> currentDeploy = deployRepo.findByServiceAndEnv(service.id(), environment.id());

        // Fast path：spec_hash 未变，仅刷新时间戳。
        if (currentDeploy.isPresent() && specHash.equals(currentDeploy.get().specHash())) {
            deployRepo.touchLastRegisteredAt(currentDeploy.get().id());
            log.debug("Spec unchanged for {} ({}), skipping normalize and upsert", serviceName, environmentCode);
            archiveRawSpec(currentDeploy.get().id(), rawSpec, request.getSpec().getFormat());
            eventPublisher.publishEvent(new ServiceRegisteredEvent(this, serviceName, environmentCode));
            return ApiRegistrationResponse.builder()
                    .accepted(true)
                    .registrationId(currentDeploy.get().id())
                    .message("Registration unchanged")
                    .build();
        }

        NormalizationResult result = normalizer.normalize(
                serviceName,
                environmentCode,
                request.getService().getTitle(),
                request.getService().getVersion(),
                request.getService().getServerUrls(),
                rawSpec);

        if (!result.getWarnings().isEmpty()) {
            log.warn("Normalization produced {} warning(s) for service {} ({})",
                    result.getWarnings().size(), serviceName, environmentCode);
        }

        DeploymentRow deployment = deployRepo.upsert(
                service.id(), environment.id(),
                result.getSnapshot().getVersion(),
                adapterType,
                request.getSpec().getFormat(),
                writeJson(result.getSnapshot().getServerUrls()),
                specHash,
                writeJson(result.getWarnings()),
                writeJson(request.getExtensions()));

        // 展平所有分组下的 operations，逐个 upsert。
        List<CanonicalOperation> flatOps = new ArrayList<>();
        Map<String, CanonicalGroup> groupByTag = new LinkedHashMap<>();
        for (CanonicalGroup group : result.getSnapshot().getGroups()) {
            groupByTag.put(group.getName(), group);
            flatOps.addAll(group.getOperations());
        }

        // upsert tags 并拿到 name → id 索引
        List<TagSpec> tagSpecs = groupByTag.values().stream()
                .map(g -> new TagSpec(g.getName(), g.getSlug()))
                .toList();
        Map<String, String> tagIdByName = tagRepo.upsertAll(service.id(), tagSpecs);

        // 当前 service 下所有 operation 的 hash 索引，用于增量比对
        Map<String, String> existingHashes = opRepo.findDefinitionHashIndex(service.id());

        Set<String> seenOpIds = new HashSet<>();
        int created = 0, structureChanged = 0, descriptionOnly = 0;

        for (CanonicalOperation op : flatOps) {
            String opKey = operationKey(op);
            String defHash = CanonicalHasher.hashOperation(op);
            String canonicalJson;
            try {
                canonicalJson = objectMapper.writeValueAsString(CanonicalHasher.toCanonicalNode(op));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize canonical operation", e);
            }

            Optional<OperationRow> existing = opRepo.findByServiceAndKey(service.id(), opKey);
            OperationRow opRow;
            if (existing.isEmpty()) {
                opRow = opRepo.insert(service.id(), opKey, op.getMethod(), op.getPath(),
                        defHash, writeJson(op.getSecurityRequirements()));
                childrenRepo.insertParameters(opRow.id(), op.getParameters());
                childrenRepo.insertRequestBody(opRow.id(), op.getRequestBody());
                childrenRepo.insertResponses(opRow.id(), op.getResponses());
                versionRepo.appendIfNew(opRow.id(), defHash, canonicalJson, deployment.id());
                created++;
            } else if (!defHash.equals(existing.get().definitionHash())) {
                opRow = existing.get();
                cleanupChildrenLocalizations(opRow.id());
                childrenRepo.deleteByOperationId(opRow.id());
                childrenRepo.insertParameters(opRow.id(), op.getParameters());
                childrenRepo.insertRequestBody(opRow.id(), op.getRequestBody());
                childrenRepo.insertResponses(opRow.id(), op.getResponses());
                opRepo.updateStructure(opRow.id(), op.getMethod(), op.getPath(), defHash,
                        writeJson(op.getSecurityRequirements()));
                versionRepo.appendIfNew(opRow.id(), defHash, canonicalJson, deployment.id());
                structureChanged++;
            } else {
                opRow = existing.get();
                descriptionOnly++;
            }

            // 按 name → id 解析当前 op 的 tag 列表，忽略未在分组阶段声明的 tag
            List<String> opTagIds = Optional.ofNullable(op.getTags()).orElse(List.of()).stream()
                    .map(tagIdByName::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            tagRepo.replaceOperationTags(opRow.id(), opTagIds);

            // 每次注册都刷新 adapter 本地化（描述可能变）
            upsertOperationLocalizations(opRow.id(), op, defaultLocale);

            seenOpIds.add(opRow.id());
            // 顺带淘汰存量 hash 中的对应项，剩下的就是本次没出现的"孤儿"
            existingHashes.remove(opKey);
        }

        deployRepo.replaceDeploymentOperations(deployment.id(), seenOpIds);

        // service 级本地化（title、description）
        localizationRepo.upsert(LocalizationEntity.SERVICE, service.id(),
                LocalizationField.TITLE, defaultLocale, LocalizationSource.ADAPTER,
                result.getSnapshot().getTitle());

        // tag 级本地化（description）
        for (CanonicalGroup group : groupByTag.values()) {
            String tagId = tagIdByName.get(group.getName());
            if (tagId == null) {
                continue;
            }
            localizationRepo.upsert(LocalizationEntity.TAG, tagId,
                    LocalizationField.DESCRIPTION, defaultLocale, LocalizationSource.ADAPTER,
                    group.getDescription());
        }

        archiveRawSpec(deployment.id(), rawSpec, request.getSpec().getFormat());

        log.info("Registered service {} ({}) deployment {} [created={}, structureChanged={}, descriptionOnly={}, orphaned={}]",
                serviceName, environmentCode, deployment.id(),
                created, structureChanged, descriptionOnly, existingHashes.size());

        eventPublisher.publishEvent(new ServiceRegisteredEvent(this, serviceName, environmentCode));

        return ApiRegistrationResponse.builder()
                .accepted(true)
                .registrationId(deployment.id())
                .message("Registration accepted")
                .warnings(result.getWarnings())
                .build();
    }

    private String resolveDefaultLocale(ApiRegistrationRequest request) {
        String declared = request.getService().getDefaultLocale();
        return (declared == null || declared.isBlank()) ? DEFAULT_LOCALE_FALLBACK : declared;
    }

    private String operationKey(CanonicalOperation op) {
        return op.getOperationId() != null && !op.getOperationId().isBlank()
                ? op.getOperationId()
                : op.getMethod().toUpperCase() + " " + op.getPath();
    }

    private void upsertOperationLocalizations(String operationId, CanonicalOperation op, String defaultLocale) {
        localizationRepo.upsert(LocalizationEntity.OPERATION, operationId,
                LocalizationField.SUMMARY, defaultLocale, LocalizationSource.ADAPTER, op.getSummary());
        localizationRepo.upsert(LocalizationEntity.OPERATION, operationId,
                LocalizationField.DESCRIPTION, defaultLocale, LocalizationSource.ADAPTER, op.getDescription());

        // 子表级本地化：参数、请求体、响应 description
        List<OperationParameterRow> params = childrenRepo.findParameters(operationId);
        List<CanonicalParameter> submitted = Optional.ofNullable(op.getParameters()).orElse(List.of());
        // (location, name) → 提交内容 的索引，方便按稳定身份对齐
        Map<String, CanonicalParameter> byKey = new HashMap<>();
        for (CanonicalParameter p : submitted) {
            byKey.put(p.getLocation() + "::" + p.getName(), p);
        }
        for (OperationParameterRow row : params) {
            CanonicalParameter p = byKey.get(row.location() + "::" + row.name());
            if (p == null) {
                continue;
            }
            localizationRepo.upsert(LocalizationEntity.OPERATION_PARAMETER, row.id(),
                    LocalizationField.DESCRIPTION, defaultLocale, LocalizationSource.ADAPTER,
                    p.getDescription());
        }

        childrenRepo.findRequestBody(operationId).ifPresent(row -> {
            // request body 本身没有 description 字段，暂不本地化；保留接入口。
        });

        List<OperationResponseRow> responses = childrenRepo.findResponses(operationId);
        Map<String, CanonicalResponse> respByStatus = new HashMap<>();
        for (CanonicalResponse r : Optional.ofNullable(op.getResponses()).orElse(List.of())) {
            respByStatus.put(r.getStatusCode(), r);
        }
        for (OperationResponseRow row : responses) {
            CanonicalResponse r = respByStatus.get(row.statusCode());
            if (r == null) {
                continue;
            }
            localizationRepo.upsert(LocalizationEntity.OPERATION_RESPONSE, row.id(),
                    LocalizationField.DESCRIPTION, defaultLocale, LocalizationSource.ADAPTER,
                    r.getDescription());
        }
    }

    /**
     * 接口结构变更时一并清理子表旧 id 对应的本地化，避免残留指向已删除子行的数据。
     */
    private void cleanupChildrenLocalizations(String operationId) {
        for (OperationParameterRow row : childrenRepo.findParameters(operationId)) {
            localizationRepo.deleteByEntity(LocalizationEntity.OPERATION_PARAMETER, row.id());
        }
        childrenRepo.findRequestBody(operationId).ifPresent(row ->
                localizationRepo.deleteByEntity(LocalizationEntity.OPERATION_REQUEST_BODY, row.id()));
        for (OperationResponseRow row : childrenRepo.findResponses(operationId)) {
            localizationRepo.deleteByEntity(LocalizationEntity.OPERATION_RESPONSE, row.id());
        }
    }

    /**
     * 把 raw spec 归档放在事务 afterCommit 钩子里执行，避免事务回滚后留下孤儿文件。
     * 事务上下文缺席时退化为同步写。
     */
    private void archiveRawSpec(String deploymentId, String rawSpec, String specFormat) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doArchive(deploymentId, rawSpec, specFormat);
                }
            });
        } else {
            doArchive(deploymentId, rawSpec, specFormat);
        }
    }

    private void doArchive(String deploymentId, String rawSpec, String specFormat) {
        try {
            fileSpecStore.saveRawSpec(deploymentId, rawSpec, specFormat);
        } catch (Exception e) {
            log.warn("Failed to archive raw spec for deployment {}", deploymentId, e);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.util.Collection<?> col && col.isEmpty()) {
            return null;
        }
        if (value instanceof java.util.Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize to JSON", e);
        }
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
