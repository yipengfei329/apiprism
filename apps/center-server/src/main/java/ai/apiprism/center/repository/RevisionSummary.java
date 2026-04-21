package ai.apiprism.center.repository;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 版本历史列表展示用的轻量摘要，不含 snapshot 大字段，适合列表渲染。
 */
@Getter
@Builder
public class RevisionSummary {

    private final String id;

    private final String serviceName;

    private final String environment;

    private final Long seq;

    private final String specHash;

    private final String title;

    private final String version;

    private final String adapterType;

    private final String source;

    private final int warningsCount;

    private final Instant registeredAt;

    private final boolean current;

    /** 该 revision 的接口总数，旧 revision 为 null。 */
    private final Integer endpointCount;

    /** 相比前驱 revision 新增的接口数，旧 revision 为 null。 */
    private final Integer addedCount;

    /** 相比前驱 revision 删除的接口数，旧 revision 为 null。 */
    private final Integer removedCount;

    /** 相比前驱 revision 变更的接口数，旧 revision 为 null。 */
    private final Integer modifiedCount;
}
