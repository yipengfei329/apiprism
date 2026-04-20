package ai.apiprism.center.revision;

import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.center.repository.RegistrationRepository;
import ai.apiprism.center.repository.RevisionSummary;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.mcp.event.ServiceRegisteredEvent;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 版本历史服务：暴露列表、查看指定 revision、回滚（activate）三个能力。
 * activate 成功后复用 ServiceRegisteredEvent 让 MCP 网关跟随刷新。
 */
@Service
public class RevisionService {

    private static final Logger log = LoggerFactory.getLogger(RevisionService.class);

    private final RegistrationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public RevisionService(RegistrationRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public List<RevisionSummary> listRevisions(String serviceName, String environment) {
        // 先确保服务存在，避免返回空列表让前端产生歧义
        repository.findCurrent(serviceName, environment)
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));
        return repository.listRevisions(serviceName, environment);
    }

    public CanonicalServiceSnapshot getRevisionSnapshot(String serviceName, String environment, String revisionId) {
        return repository.findRevision(serviceName, environment, revisionId)
                .map(StoredRegistration::getSnapshot)
                .orElseThrow(() -> new ai.apiprism.center.exceptions.RevisionNotFoundException(
                        serviceName, environment, revisionId));
    }

    /**
     * 读取指定 revision 下某个分组。先按 slug 精确匹配，再 fallback 到 name，
     * 行为与 CatalogService.getGroupBySlug 保持一致。
     */
    public CanonicalGroup getRevisionGroup(String serviceName, String environment, String revisionId, String groupSlug) {
        List<CanonicalGroup> groups = getRevisionSnapshot(serviceName, environment, revisionId).getGroups();
        return groups.stream()
                .filter(g -> groupSlug.equals(g.getSlug()))
                .findFirst()
                .or(() -> groups.stream().filter(g -> g.getName().equals(groupSlug)).findFirst())
                .orElseThrow(() -> new ai.apiprism.center.exceptions.RevisionNotFoundException(
                        serviceName, environment, revisionId));
    }

    /**
     * 读取指定 revision 下某个接口。
     */
    public CanonicalOperation getRevisionOperation(String serviceName, String environment, String revisionId, String operationId) {
        return getRevisionSnapshot(serviceName, environment, revisionId).getGroups().stream()
                .flatMap(group -> group.getOperations().stream())
                .filter(op -> op.getOperationId().equals(operationId))
                .findFirst()
                .orElseThrow(() -> new ai.apiprism.center.exceptions.RevisionNotFoundException(
                        serviceName, environment, revisionId));
    }

    public ActivateResult activate(String serviceName, String environment, String revisionId) {
        StoredRegistration previousCurrent = repository.findCurrent(serviceName, environment)
                .orElseThrow(() -> new RegistrationNotFoundException(serviceName, environment));

        if (previousCurrent.getId().equals(revisionId)) {
            log.debug("Activate no-op: {} already current for {} ({})", revisionId, serviceName, environment);
            return new ActivateResult(previousCurrent, false);
        }

        StoredRegistration activated = repository.activateRevision(serviceName, environment, revisionId);
        log.info("Rolled back {} ({}) from revision {} to {}",
                serviceName, environment, previousCurrent.getId(), activated.getId());

        // 通知 MCP 网关刷新，与 adapter 注册路径共用事件
        eventPublisher.publishEvent(new ServiceRegisteredEvent(this, serviceName, environment));
        return new ActivateResult(activated, true);
    }

    public record ActivateResult(StoredRegistration current, boolean changed) {
    }
}
