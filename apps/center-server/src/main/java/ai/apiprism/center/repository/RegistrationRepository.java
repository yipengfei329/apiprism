package ai.apiprism.center.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 注册数据的持久化抽象，解耦具体存储实现。
 * 同时承担版本历史的读写职责：每次 spec hash 变化追加一条 revision，
 * current pointer 指向其中一条，查询默认返回 current。
 */
public interface RegistrationRepository {

    /**
     * 保存一次 adapter 注册请求，按版本策略追加或复用 revision：
     * - 若与 current 的 specHash 相同，刷新 last_seen_at，返回现有 current。
     * - 否则追加新 revision 并将 current pointer 指向新 revision。
     */
    StoredRegistration saveRevision(StoredRegistration incoming);

    /** 读取 current pointer 对应的 revision。 */
    Optional<StoredRegistration> findCurrent(String serviceName, String environment);

    /** 读取指定历史 revision，若 revisionId 不属于 (service, env) 返回空。 */
    Optional<StoredRegistration> findRevision(String serviceName, String environment, String revisionId);

    /** 列出 (service, env) 下所有历史 revision，按 seq 降序。 */
    List<RevisionSummary> listRevisions(String serviceName, String environment);

    /** 将 current pointer 切换到指定 revision（幂等），返回切换后的 current。 */
    StoredRegistration activateRevision(String serviceName, String environment, String revisionId);

    /** 返回所有 service+env 的 current revision，CatalogService 用来渲染服务列表。 */
    Collection<StoredRegistration> findAll();

    /** 彻底删除一个 service+env 下的所有历史 revision、current pointer 及磁盘文件。 */
    void deleteByRef(String serviceName, String environment);

    // ---- 向后兼容：委派到新接口，避免大面积改动调用点 ----

    default StoredRegistration save(StoredRegistration registration) {
        return saveRevision(registration);
    }

    default Optional<StoredRegistration> findByRef(String serviceName, String environment) {
        return findCurrent(serviceName, environment);
    }
}
