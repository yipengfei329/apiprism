package ai.apiprism.center.repository;

import java.util.Collection;
import java.util.Optional;

/**
 * 注册数据的持久化抽象，解耦具体存储实现。
 */
public interface RegistrationRepository {

    StoredRegistration save(StoredRegistration registration);

    Optional<StoredRegistration> findByRef(String serviceName, String environment);

    Collection<StoredRegistration> findAll();
}
