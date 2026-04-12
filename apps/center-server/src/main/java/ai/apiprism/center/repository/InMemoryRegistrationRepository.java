package ai.apiprism.center.repository;

import ai.apiprism.model.ServiceRef;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存实现，仅在 apiprism.storage.type=memory 时激活，用于测试或无持久化需求场景。
 */
@Repository
@ConditionalOnProperty(name = "apiprism.storage.type", havingValue = "memory")
public class InMemoryRegistrationRepository implements RegistrationRepository {

    private final Map<String, StoredRegistration> registrations = new ConcurrentHashMap<>();

    @Override
    public StoredRegistration save(StoredRegistration registration) {
        registrations.put(key(registration.getSnapshot().getRef()), registration);
        return registration;
    }

    @Override
    public Optional<StoredRegistration> findByRef(String serviceName, String environment) {
        return Optional.ofNullable(registrations.get(key(ServiceRef.builder()
                .name(serviceName)
                .environment(environment)
                .build())));
    }

    @Override
    public Collection<StoredRegistration> findAll() {
        return registrations.values();
    }

    private String key(ServiceRef ref) {
        return ref.getName() + "::" + ref.getEnvironment();
    }
}
