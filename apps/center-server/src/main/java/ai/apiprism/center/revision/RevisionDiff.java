package ai.apiprism.center.revision;

import java.util.List;

/**
 * 两个相邻 revision 之间的接口变更统计。
 * 首个 revision（无前驱）的 added 包含全部接口，removed/modified 为空。
 */
public record RevisionDiff(
        int totalEndpoints,
        List<EndpointRef> added,
        List<EndpointRef> removed,
        List<EndpointRef> modified
) {

    public record EndpointRef(String method, String path, String operationId, String summary) {
    }
}
