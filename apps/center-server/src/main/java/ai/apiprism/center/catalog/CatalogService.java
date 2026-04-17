package ai.apiprism.center.catalog;

import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.center.localization.LocalizationEntity;
import ai.apiprism.center.localization.LocalizationField;
import ai.apiprism.center.localization.LocalizationResolver;
import ai.apiprism.center.localization.LocalizationResolver.ResolverKey;
import ai.apiprism.center.registration.persistence.DeploymentRepository;
import ai.apiprism.center.registration.persistence.EnvironmentRepository;
import ai.apiprism.center.registration.persistence.OperationChildrenRepository;
import ai.apiprism.center.registration.persistence.OperationRepository;
import ai.apiprism.center.registration.persistence.PersistenceRows.DeploymentRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.EnvironmentRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationParameterRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationRequestBodyRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationResponseRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.ServiceRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.TagRow;
import ai.apiprism.center.registration.persistence.ServiceRepository;
import ai.apiprism.center.registration.persistence.TagRepository;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 目录查询服务：从规范化表组装 CanonicalServiceSnapshot，
 * 描述类字段走 LocalizationResolver 按 (userLocale, source) 优先级解析。
 *
 * <p>渲染层（MarkdownRenderer）和 MCP 网关沿用老的 CanonicalServiceSnapshot 形状，
 * 本服务只负责"把拆开的表重新拼回去"。
 */
@Service
public class CatalogService {

    private static final TypeReference<Map<String, Object>> SCHEMA_REF = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_REF = new TypeReference<>() {
    };

    private final ServiceRepository serviceRepo;
    private final EnvironmentRepository envRepo;
    private final DeploymentRepository deployRepo;
    private final TagRepository tagRepo;
    private final OperationRepository opRepo;
    private final OperationChildrenRepository childrenRepo;
    private final LocalizationResolver localizationResolver;
    private final ObjectMapper objectMapper;

    public CatalogService(ServiceRepository serviceRepo,
                          EnvironmentRepository envRepo,
                          DeploymentRepository deployRepo,
                          TagRepository tagRepo,
                          OperationRepository opRepo,
                          OperationChildrenRepository childrenRepo,
                          LocalizationResolver localizationResolver,
                          ObjectMapper objectMapper) {
        this.serviceRepo = serviceRepo;
        this.envRepo = envRepo;
        this.deployRepo = deployRepo;
        this.tagRepo = tagRepo;
        this.opRepo = opRepo;
        this.childrenRepo = childrenRepo;
        this.localizationResolver = localizationResolver;
        this.objectMapper = objectMapper;
    }

    public List<ServiceCatalogItem> listServices() {
        return listServices(null);
    }

    public List<ServiceCatalogItem> listServices(String userLocale) {
        List<DeploymentRow> deployments = deployRepo.findAll();
        if (deployments.isEmpty()) {
            return List.of();
        }
        Map<String, ServiceRow> servicesById = new HashMap<>();
        Map<String, EnvironmentRow> envsById = new HashMap<>();
        for (ServiceRow s : serviceRepo.findAll()) {
            servicesById.put(s.id(), s);
        }
        for (EnvironmentRow e : envRepo.findAll()) {
            envsById.put(e.id(), e);
        }

        List<ServiceCatalogItem> items = new ArrayList<>(deployments.size());
        for (DeploymentRow deploy : deployments) {
            ServiceRow svc = servicesById.get(deploy.serviceId());
            EnvironmentRow env = envsById.get(deploy.environmentId());
            if (svc == null || env == null) {
                continue;
            }
            String effectiveLocale = userLocale == null ? svc.defaultLocale() : userLocale;
            String title = localizationResolver.resolve(LocalizationEntity.SERVICE, svc.id(),
                    LocalizationField.TITLE, effectiveLocale, svc.defaultLocale())
                    .orElse(svc.name());
            List<ServiceCatalogItem.GroupRef> groups = loadGroupRefs(svc.id(), deploy.id());
            items.add(ServiceCatalogItem.builder()
                    .name(svc.name())
                    .environment(env.code())
                    .title(title)
                    .version(deploy.version())
                    .updatedAt(deploy.lastRegisteredAt())
                    .groups(groups)
                    .build());
        }
        items.sort(Comparator.comparing(ServiceCatalogItem::getName, String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    public CanonicalServiceSnapshot getService(String serviceName, String environment) {
        return getService(serviceName, environment, null);
    }

    public CanonicalServiceSnapshot getService(String serviceName, String environment, String userLocale) {
        ServiceRow service = serviceRepo.findByName(serviceName)
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
        EnvironmentRow env = envRepo.findByCode(environment)
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
        DeploymentRow deploy = deployRepo.findByServiceAndEnv(service.id(), env.id())
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));

        String effectiveLocale = userLocale == null ? service.defaultLocale() : userLocale;

        List<String> opIds = deployRepo.findDeploymentOperationIds(deploy.id());
        List<OperationRow> ops = opRepo.findByIds(opIds);
        List<TagRow> tags = tagRepo.findByServiceId(service.id());
        Map<String, TagRow> tagsById = new HashMap<>();
        for (TagRow t : tags) {
            tagsById.put(t.id(), t);
        }

        Map<String, List<String>> tagIdsByOp = tagRepo.findTagIdsByOperationIds(
                ops.stream().map(OperationRow::id).toList());
        Map<String, List<OperationParameterRow>> paramsByOp = childrenRepo.findParametersByOperationIds(
                ops.stream().map(OperationRow::id).toList());
        Map<String, OperationRequestBodyRow> bodyByOp = childrenRepo.findRequestBodiesByOperationIds(
                ops.stream().map(OperationRow::id).toList());
        Map<String, List<OperationResponseRow>> responsesByOp = childrenRepo.findResponsesByOperationIds(
                ops.stream().map(OperationRow::id).toList());

        // 批量准备本地化 key
        List<ResolverKey> keys = new ArrayList<>();
        keys.add(new ResolverKey(LocalizationEntity.SERVICE, service.id(), LocalizationField.TITLE));
        for (TagRow t : tags) {
            keys.add(new ResolverKey(LocalizationEntity.TAG, t.id(), LocalizationField.DESCRIPTION));
        }
        for (OperationRow op : ops) {
            keys.add(new ResolverKey(LocalizationEntity.OPERATION, op.id(), LocalizationField.SUMMARY));
            keys.add(new ResolverKey(LocalizationEntity.OPERATION, op.id(), LocalizationField.DESCRIPTION));
            for (OperationParameterRow p : paramsByOp.getOrDefault(op.id(), List.of())) {
                keys.add(new ResolverKey(LocalizationEntity.OPERATION_PARAMETER, p.id(),
                        LocalizationField.DESCRIPTION));
            }
            for (OperationResponseRow r : responsesByOp.getOrDefault(op.id(), List.of())) {
                keys.add(new ResolverKey(LocalizationEntity.OPERATION_RESPONSE, r.id(),
                        LocalizationField.DESCRIPTION));
            }
        }
        Map<ResolverKey, String> localized = localizationResolver.resolveBatch(
                keys, effectiveLocale, service.defaultLocale());

        String title = pick(localized, LocalizationEntity.SERVICE, service.id(), LocalizationField.TITLE, service.name());

        // 按 tag 组装 group；没有任何 tag 的接口归入隐式 default 组，维持老逻辑兼容。
        Map<String, List<CanonicalOperation>> groupedOps = new LinkedHashMap<>();
        Map<String, TagRow> firstTagForGroup = new LinkedHashMap<>();
        for (OperationRow op : ops) {
            CanonicalOperation canonical = buildCanonicalOperation(
                    op, paramsByOp.getOrDefault(op.id(), List.of()),
                    bodyByOp.get(op.id()),
                    responsesByOp.getOrDefault(op.id(), List.of()),
                    tagIdsByOp.getOrDefault(op.id(), List.of()).stream()
                            .map(tagsById::get)
                            .filter(java.util.Objects::nonNull)
                            .toList(),
                    localized);

            List<String> tagNames = canonical.getTags();
            String groupName = (tagNames == null || tagNames.isEmpty()) ? "default" : tagNames.get(0);
            groupedOps.computeIfAbsent(groupName, k -> new ArrayList<>()).add(canonical);
            if (!firstTagForGroup.containsKey(groupName) && tagNames != null && !tagNames.isEmpty()) {
                tagsById.values().stream()
                        .filter(t -> t.name().equals(groupName))
                        .findFirst()
                        .ifPresent(t -> firstTagForGroup.put(groupName, t));
            }
        }

        List<CanonicalGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<CanonicalOperation>> entry : groupedOps.entrySet()) {
            TagRow tag = firstTagForGroup.get(entry.getKey());
            String groupDesc = tag == null ? null
                    : localized.get(new ResolverKey(LocalizationEntity.TAG, tag.id(), LocalizationField.DESCRIPTION));
            groups.add(CanonicalGroup.builder()
                    .name(entry.getKey())
                    .slug(tag == null ? null : tag.slug())
                    .description(groupDesc)
                    .operations(entry.getValue())
                    .build());
        }

        return CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name(service.name()).environment(env.code()).build())
                .title(title)
                .version(deploy.version() == null ? "unknown" : deploy.version())
                .serverUrls(parseServerUrls(deploy.serverUrlsJson()))
                .groups(groups)
                .updatedAt(deploy.lastRegisteredAt())
                .build();
    }

    public CanonicalGroup getGroup(String serviceName, String environment, String groupName) {
        return getGroup(serviceName, environment, groupName, null);
    }

    public CanonicalGroup getGroup(String serviceName, String environment, String groupName, String userLocale) {
        return getService(serviceName, environment, userLocale).getGroups().stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    public CanonicalGroup getGroupBySlug(String serviceName, String environment, String slug) {
        return getGroupBySlug(serviceName, environment, slug, null);
    }

    public CanonicalGroup getGroupBySlug(String serviceName, String environment, String slug, String userLocale) {
        List<CanonicalGroup> groups = getService(serviceName, environment, userLocale).getGroups();
        return groups.stream()
                .filter(g -> slug.equals(g.getSlug()))
                .findFirst()
                .or(() -> groups.stream().filter(g -> g.getName().equals(slug)).findFirst())
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    public CanonicalOperation getOperation(String serviceName, String environment, String operationId) {
        return getOperation(serviceName, environment, operationId, null);
    }

    public CanonicalOperation getOperation(String serviceName, String environment,
                                           String operationId, String userLocale) {
        return getService(serviceName, environment, userLocale).getGroups().stream()
                .flatMap(g -> g.getOperations().stream())
                .filter(op -> op.getOperationId().equals(operationId))
                .findFirst()
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    private CanonicalOperation buildCanonicalOperation(OperationRow row,
                                                       List<OperationParameterRow> paramRows,
                                                       OperationRequestBodyRow bodyRow,
                                                       List<OperationResponseRow> responseRows,
                                                       List<TagRow> tags,
                                                       Map<ResolverKey, String> localized) {
        String summary = pick(localized, LocalizationEntity.OPERATION, row.id(),
                LocalizationField.SUMMARY, null);
        String description = pick(localized, LocalizationEntity.OPERATION, row.id(),
                LocalizationField.DESCRIPTION, null);

        CanonicalOperation.CanonicalOperationBuilder builder = CanonicalOperation.builder()
                .operationId(row.operationKey())
                .method(row.method())
                .path(row.path())
                .summary(summary)
                .description(description);
        for (TagRow t : tags) {
            builder.tag(t.name());
        }
        for (String sec : parseStringList(row.securityRequirementsJson())) {
            builder.securityRequirement(sec);
        }
        for (OperationParameterRow p : paramRows) {
            String pDesc = localized.get(new ResolverKey(LocalizationEntity.OPERATION_PARAMETER,
                    p.id(), LocalizationField.DESCRIPTION));
            builder.parameter(CanonicalParameter.builder()
                    .name(p.name())
                    .location(p.location())
                    .required(p.required())
                    .schema(parseSchema(p.schemaJson()))
                    .description(pDesc)
                    .build());
        }
        if (bodyRow != null) {
            builder.requestBody(CanonicalRequestBody.builder()
                    .contentType(bodyRow.contentType())
                    .required(bodyRow.required())
                    .schema(parseSchema(bodyRow.schemaJson()))
                    .build());
        }
        for (OperationResponseRow r : responseRows) {
            String rDesc = localized.get(new ResolverKey(LocalizationEntity.OPERATION_RESPONSE,
                    r.id(), LocalizationField.DESCRIPTION));
            builder.response(CanonicalResponse.builder()
                    .statusCode(r.statusCode())
                    .contentType(r.contentType())
                    .schema(parseSchema(r.schemaJson()))
                    .description(rDesc)
                    .build());
        }
        return builder.build();
    }

    private List<ServiceCatalogItem.GroupRef> loadGroupRefs(String serviceId, String deploymentId) {
        List<String> opIds = deployRepo.findDeploymentOperationIds(deploymentId);
        if (opIds.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> tagIdsByOp = tagRepo.findTagIdsByOperationIds(opIds);
        Map<String, TagRow> tagsById = new HashMap<>();
        for (TagRow t : tagRepo.findByServiceId(serviceId)) {
            tagsById.put(t.id(), t);
        }
        Map<String, ServiceCatalogItem.GroupRef> groupByName = new LinkedHashMap<>();
        for (String opId : opIds) {
            List<String> tagIds = tagIdsByOp.getOrDefault(opId, List.of());
            if (tagIds.isEmpty()) {
                groupByName.computeIfAbsent("default",
                        k -> ServiceCatalogItem.GroupRef.builder().name("default").slug("default").build());
                continue;
            }
            TagRow first = tagsById.get(tagIds.get(0));
            if (first != null) {
                groupByName.computeIfAbsent(first.name(),
                        k -> ServiceCatalogItem.GroupRef.builder().name(first.name()).slug(first.slug()).build());
            }
        }
        return new ArrayList<>(groupByName.values());
    }

    private static String pick(Map<ResolverKey, String> localized,
                               LocalizationEntity entity, String id, String field, String fallback) {
        String hit = localized.get(new ResolverKey(entity, id, field));
        return hit != null ? hit : fallback;
    }

    private Map<String, Object> parseSchema(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SCHEMA_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize schema JSON", e);
        }
    }

    private List<String> parseServerUrls(String json) {
        return parseStringList(json);
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_REF);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
