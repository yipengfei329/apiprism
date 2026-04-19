package ai.apiprism.center.repository;

import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.center.exceptions.RevisionNotFoundException;
import ai.apiprism.model.ServiceRef;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存实现，仅在 apiprism.storage.type=memory 时激活。
 * 每个 service+env 维护一份按追加顺序排列的 revision 列表，以及指向其中之一的 current id。
 */
@Repository
@ConditionalOnProperty(name = "apiprism.storage.type", havingValue = "memory")
public class InMemoryRegistrationRepository implements RegistrationRepository {

    private static final String SOURCE_REGISTER = "REGISTER";

    private final Map<String, List<StoredRegistration>> revisions = new ConcurrentHashMap<>();
    private final Map<String, String> currentIds = new ConcurrentHashMap<>();

    @Override
    public synchronized StoredRegistration saveRevision(StoredRegistration incoming) {
        String key = keyOf(incoming.getSnapshot().getRef());
        List<StoredRegistration> history = revisions.computeIfAbsent(key, k -> new ArrayList<>());
        String currentId = currentIds.get(key);

        // 与 current 相同 hash：不追加
        if (currentId != null) {
            StoredRegistration current = findInHistory(history, currentId);
            if (current != null && current.getSpecHash().equals(incoming.getSpecHash())) {
                return current;
            }
        }

        long seq = history.size() + 1L;
        StoredRegistration appended = incoming.toBuilder()
                .revisionSeq(seq)
                .source(SOURCE_REGISTER)
                .registeredAt(Instant.now())
                .current(true)
                .build();
        history.add(appended);
        currentIds.put(key, appended.getId());
        return appended;
    }

    @Override
    public Optional<StoredRegistration> findCurrent(String serviceName, String environment) {
        String key = keyOf(serviceName, environment);
        String currentId = currentIds.get(key);
        if (currentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(findInHistory(revisions.get(key), currentId))
                .map(r -> withCurrent(r, currentId));
    }

    @Override
    public Optional<StoredRegistration> findRevision(String serviceName, String environment, String revisionId) {
        String key = keyOf(serviceName, environment);
        String currentId = currentIds.get(key);
        List<StoredRegistration> history = revisions.get(key);
        if (history == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(findInHistory(history, revisionId))
                .map(r -> withCurrent(r, currentId));
    }

    @Override
    public List<RevisionSummary> listRevisions(String serviceName, String environment) {
        String key = keyOf(serviceName, environment);
        String currentId = currentIds.get(key);
        List<StoredRegistration> history = revisions.getOrDefault(key, List.of());
        return history.stream()
                .sorted(Comparator.comparing(StoredRegistration::getRevisionSeq).reversed())
                .map(r -> RevisionSummary.builder()
                        .id(r.getId())
                        .serviceName(serviceName)
                        .environment(environment)
                        .seq(r.getRevisionSeq())
                        .specHash(r.getSpecHash())
                        .title(r.getSnapshot().getTitle())
                        .version(r.getSnapshot().getVersion())
                        .adapterType(r.getAdapterType())
                        .source(r.getSource() == null ? SOURCE_REGISTER : r.getSource())
                        .warningsCount(r.getWarnings() == null ? 0 : r.getWarnings().size())
                        .registeredAt(r.getRegisteredAt())
                        .current(r.getId().equals(currentId))
                        .build())
                .toList();
    }

    @Override
    public synchronized StoredRegistration activateRevision(String serviceName, String environment, String revisionId) {
        String key = keyOf(serviceName, environment);
        if (!currentIds.containsKey(key)) {
            throw new RegistrationNotFoundException(serviceName, environment);
        }
        List<StoredRegistration> history = revisions.getOrDefault(key, List.of());
        StoredRegistration target = findInHistory(history, revisionId);
        if (target == null) {
            throw new RevisionNotFoundException(serviceName, environment, revisionId);
        }
        currentIds.put(key, revisionId);
        return withCurrent(target, revisionId);
    }

    @Override
    public Collection<StoredRegistration> findAll() {
        List<StoredRegistration> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : currentIds.entrySet()) {
            StoredRegistration current = findInHistory(revisions.get(entry.getKey()), entry.getValue());
            if (current != null) {
                result.add(withCurrent(current, entry.getValue()));
            }
        }
        return result;
    }

    @Override
    public synchronized void deleteByRef(String serviceName, String environment) {
        String key = keyOf(serviceName, environment);
        revisions.remove(key);
        currentIds.remove(key);
    }

    private StoredRegistration findInHistory(List<StoredRegistration> history, String revisionId) {
        if (history == null) {
            return null;
        }
        for (StoredRegistration r : history) {
            if (r.getId().equals(revisionId)) {
                return r;
            }
        }
        return null;
    }

    private StoredRegistration withCurrent(StoredRegistration r, String currentId) {
        boolean isCurrent = r.getId().equals(currentId);
        if (r.isCurrent() == isCurrent) {
            return r;
        }
        return r.toBuilder().current(isCurrent).build();
    }

    private String keyOf(ServiceRef ref) {
        return keyOf(ref.getName(), ref.getEnvironment());
    }

    private String keyOf(String serviceName, String environment) {
        return serviceName + "::" + environment;
    }
}
