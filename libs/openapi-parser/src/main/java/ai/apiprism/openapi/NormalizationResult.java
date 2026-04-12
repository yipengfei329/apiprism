package ai.apiprism.openapi;

import ai.apiprism.model.CanonicalServiceSnapshot;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * 归一化处理输出的快照及告警集合。
 */
@Value
@Builder(toBuilder = true)
public class NormalizationResult {

    /**
     * 归一化后的服务快照。
     */
    @NonNull
    CanonicalServiceSnapshot snapshot;

    /**
     * 归一化阶段产生的非阻断告警。
     */
    @Singular(ignoreNullCollections = true)
    List<String> warnings;
}
