package ai.apiprism.center.catalog;

import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.center.repository.RegistrationRepository;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.openapi.OpenApiNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 目录查询服务：提供服务列表、快照查询、分组与接口检索能力。
 */
@Service
public class CatalogService {

    private final RegistrationRepository repository;
    private final TagOrderRepository tagOrderRepository;

    public CatalogService(RegistrationRepository repository, TagOrderRepository tagOrderRepository) {
        this.repository = repository;
        this.tagOrderRepository = tagOrderRepository;
    }

    public List<ServiceCatalogItem> listServices() {
        return repository.findAll().stream()
                .map(this::toCatalogItem)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
    }

    public CanonicalServiceSnapshot getService(String serviceName, String environment) {
        CanonicalServiceSnapshot snapshot = require(serviceName, environment).getSnapshot();
        snapshot = ensureSlugs(snapshot);
        return applyTagOrder(snapshot, serviceName, environment);
    }

    public StoredRegistration getRegistration(String serviceName, String environment) {
        return require(serviceName, environment);
    }

    public void deleteService(String serviceName, String environment) {
        require(serviceName, environment);
        repository.deleteByRef(serviceName, environment);
    }

    public List<String> listEnvironments(String serviceName) {
        return repository.findAll().stream()
                .filter(r -> r.getSnapshot().getRef().getName().equals(serviceName))
                .map(r -> r.getSnapshot().getRef().getEnvironment())
                .sorted()
                .toList();
    }

    public CanonicalGroup getGroup(String serviceName, String environment, String groupName) {
        return getService(serviceName, environment).getGroups().stream()
                .filter(candidate -> candidate.getName().equals(groupName))
                .findFirst()
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    /**
     * 通过 slug 查找分组：先精确匹配 slug，再 fallback 到 name。
     */
    public CanonicalGroup getGroupBySlug(String serviceName, String environment, String slug) {
        List<CanonicalGroup> groups = getService(serviceName, environment).getGroups();
        return groups.stream()
                .filter(g -> slug.equals(g.getSlug()))
                .findFirst()
                .or(() -> groups.stream().filter(g -> g.getName().equals(slug)).findFirst())
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    public CanonicalOperation getOperation(String serviceName, String environment, String operationId) {
        return getService(serviceName, environment).getGroups().stream()
                .flatMap(group -> group.getOperations().stream())
                .filter(operation -> operation.getOperationId().equals(operationId))
                .findFirst()
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    private StoredRegistration require(String serviceName, String environment) {
        return repository.findByRef(serviceName, environment)
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
    }

    /**
     * 按用户保存的排序重排分组。未出现在保存顺序中的新分组追加到末尾（spec 升级时自然处理）。
     */
    private CanonicalServiceSnapshot applyTagOrder(CanonicalServiceSnapshot snapshot,
                                                    String serviceName, String environment) {
        Map<String, Integer> order = tagOrderRepository.findOrder(serviceName, environment);
        if (order.isEmpty()) {
            return snapshot;
        }
        List<CanonicalGroup> sorted = new ArrayList<>(snapshot.getGroups());
        sorted.sort(Comparator.comparingInt(g -> order.getOrDefault(g.getSlug(), Integer.MAX_VALUE)));
        return snapshot.toBuilder().clearGroups().groups(sorted).build();
    }

    /**
     * 为旧数据补全缺失的 group slug。
     * 新注册的数据在 OpenApiNormalizer 中已生成 slug，此处仅处理 null 的情况。
     */
    private CanonicalServiceSnapshot ensureSlugs(CanonicalServiceSnapshot snapshot) {
        boolean allPresent = snapshot.getGroups().stream().allMatch(g -> g.getSlug() != null);
        if (allPresent) {
            return snapshot;
        }
        Set<String> usedSlugs = new HashSet<>();
        List<CanonicalGroup> patched = snapshot.getGroups().stream()
                .map(g -> {
                    if (g.getSlug() != null) {
                        usedSlugs.add(g.getSlug());
                        return g;
                    }
                    String slug = OpenApiNormalizer.slugify(g.getName());
                    while (usedSlugs.contains(slug)) {
                        slug = slug + "-2";
                    }
                    usedSlugs.add(slug);
                    return g.toBuilder().slug(slug).build();
                })
                .toList();
        return snapshot.toBuilder().clearGroups().groups(patched).build();
    }

    private ServiceCatalogItem toCatalogItem(StoredRegistration registration) {
        CanonicalServiceSnapshot snapshot = ensureSlugs(registration.getSnapshot());
        String serviceName = snapshot.getRef().getName();
        String environment = snapshot.getRef().getEnvironment();
        Map<String, Integer> order = tagOrderRepository.findOrder(serviceName, environment);
        List<CanonicalGroup> groups = snapshot.getGroups();
        if (!order.isEmpty()) {
            groups = new ArrayList<>(groups);
            groups.sort(Comparator.comparingInt(g -> order.getOrDefault(g.getSlug(), Integer.MAX_VALUE)));
        }
        int operationCount = groups.stream()
                .mapToInt(g -> g.getOperations() == null ? 0 : g.getOperations().size())
                .sum();
        return ServiceCatalogItem.builder()
                .name(serviceName)
                .environment(environment)
                .title(snapshot.getTitle())
                .version(snapshot.getVersion())
                .updatedAt(snapshot.getUpdatedAt())
                .groups(groups.stream()
                        .map(g -> ServiceCatalogItem.GroupRef.builder()
                                .name(g.getName())
                                .slug(g.getSlug())
                                .operationCount(g.getOperations() == null ? 0 : g.getOperations().size())
                                .build())
                        .toList())
                .operationCount(operationCount)
                .build();
    }
}
