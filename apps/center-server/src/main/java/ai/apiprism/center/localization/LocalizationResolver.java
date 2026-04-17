package ai.apiprism.center.localization;

import ai.apiprism.center.localization.LocalizationRepository.EntityKey;
import ai.apiprism.center.localization.LocalizationRepository.LocalizationRow;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 本地化内容读取解析器。
 *
 * <p>优先级自高到低：
 * <pre>
 * (userLocale, CENTER) &gt; (userLocale, ADAPTER) &gt; (defaultLocale, CENTER) &gt; (defaultLocale, ADAPTER)
 * </pre>
 * 任一命中即返回，跳过剩余层级。
 */
@Service
public class LocalizationResolver {

    private final LocalizationRepository repository;

    public LocalizationResolver(LocalizationRepository repository) {
        this.repository = repository;
    }

    public Optional<String> resolve(LocalizationEntity entity,
                                    String entityId,
                                    String field,
                                    String userLocale,
                                    String defaultLocale) {
        List<LocalizationRow> rows = repository.findByEntity(entity, entityId);
        return pickBest(filterField(rows, field), userLocale, defaultLocale);
    }

    /**
     * 批量解析多个 (entity, field) 组合。对同一 entity 只发一次查询，进程内按字段/locale/source 分拣。
     */
    public Map<ResolverKey, String> resolveBatch(Collection<ResolverKey> keys,
                                                 String userLocale,
                                                 String defaultLocale) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        java.util.Set<EntityKey> entityKeys = new java.util.HashSet<>();
        for (ResolverKey k : keys) {
            entityKeys.add(new EntityKey(k.entity(), k.entityId()));
        }
        Map<EntityKey, List<LocalizationRow>> rowsByEntity = repository.findByEntities(entityKeys);

        Map<ResolverKey, String> result = new HashMap<>();
        for (ResolverKey k : keys) {
            List<LocalizationRow> rows = rowsByEntity.getOrDefault(
                    new EntityKey(k.entity(), k.entityId()), List.of());
            pickBest(filterField(rows, k.field()), userLocale, defaultLocale)
                    .ifPresent(v -> result.put(k, v));
        }
        return result;
    }

    private static List<LocalizationRow> filterField(List<LocalizationRow> rows, String field) {
        return rows.stream().filter(r -> r.field().equals(field)).toList();
    }

    private static Optional<String> pickBest(List<LocalizationRow> rows,
                                             String userLocale,
                                             String defaultLocale) {
        // 按 (locale, source) 精确查找
        Optional<String> hit = find(rows, userLocale, LocalizationSource.CENTER);
        if (hit.isPresent()) return hit;
        hit = find(rows, userLocale, LocalizationSource.ADAPTER);
        if (hit.isPresent()) return hit;
        if (defaultLocale != null && !defaultLocale.equals(userLocale)) {
            hit = find(rows, defaultLocale, LocalizationSource.CENTER);
            if (hit.isPresent()) return hit;
            hit = find(rows, defaultLocale, LocalizationSource.ADAPTER);
            if (hit.isPresent()) return hit;
        }
        return Optional.empty();
    }

    private static Optional<String> find(List<LocalizationRow> rows,
                                         String locale,
                                         LocalizationSource source) {
        return rows.stream()
                .filter(r -> r.locale().equals(locale) && r.source().equals(source.code()))
                .map(LocalizationRow::content)
                .findFirst();
    }

    public record ResolverKey(LocalizationEntity entity, String entityId, String field) {
    }
}
