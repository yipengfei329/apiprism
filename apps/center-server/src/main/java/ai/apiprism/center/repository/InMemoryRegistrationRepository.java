package ai.apiprism.center.repository;

import ai.apiprism.model.ServiceRef;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
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
