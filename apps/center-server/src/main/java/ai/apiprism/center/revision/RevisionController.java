package ai.apiprism.center.revision;

import ai.apiprism.center.repository.RevisionSummary;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RevisionController {

    private final RevisionService revisionService;

    public RevisionController(RevisionService revisionService) {
        this.revisionService = revisionService;
    }

    @GetMapping("/services/{service}/env/{environment}/rev")
    public List<RevisionSummary> listRevisions(
            @PathVariable String service,
            @PathVariable String environment
    ) {
        return revisionService.listRevisions(service, environment);
    }

    @GetMapping("/services/{service}/env/{environment}/rev/{revisionId}")
    public CanonicalServiceSnapshot getRevision(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String revisionId
    ) {
        return revisionService.getRevisionSnapshot(service, environment, revisionId);
    }

    @PostMapping("/services/{service}/env/{environment}/rev/{revisionId}/activate")
    public ActivateResponse activate(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String revisionId
    ) {
        RevisionService.ActivateResult result = revisionService.activate(service, environment, revisionId);
        StoredRegistration current = result.current();
        return new ActivateResponse(
                current.getId(),
                current.getRevisionSeq(),
                current.getSpecHash(),
                current.getRegisteredAt(),
                result.changed());
    }

    public record ActivateResponse(
            String currentRevisionId,
            Long seq,
            String specHash,
            Instant registeredAt,
            boolean changed
    ) {
    }
}
