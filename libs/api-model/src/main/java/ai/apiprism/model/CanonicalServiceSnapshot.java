package ai.apiprism.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/**
 * 中心内部持久化和查询使用的规范化服务快照。
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class CanonicalServiceSnapshot {

    /**
     * 服务身份信息。
     */
    @NonNull
    ServiceRef ref;

    /**
     * 展示标题。
     */
    @NonNull
    String title;

    /**
     * 版本号。
     */
    @NonNull
    String version;

    /**
     * 服务对外访问地址列表。
     */
    @Singular(value = "serverUrl", ignoreNullCollections = true)
    List<String> serverUrls;

    /**
     * 按标签聚合后的分组列表。
     */
    @Singular(value = "group", ignoreNullCollections = true)
    List<CanonicalGroup> groups;

    /**
     * 快照生成时间。
     */
    @NonNull
    Instant updatedAt;
}
