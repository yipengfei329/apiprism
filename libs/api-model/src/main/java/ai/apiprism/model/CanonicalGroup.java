package ai.apiprism.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * 归一化后按标签聚合出来的接口分组。
 */
@Value
@Builder(toBuilder = true)
public class CanonicalGroup {

    /**
     * 分组名，通常来自首个 OpenAPI tag。
     */
    @NonNull
    String name;

    /**
     * 分组说明。
     */
    String description;

    /**
     * 分组下的操作列表。
     */
    @Singular(value = "operation", ignoreNullCollections = true)
    List<CanonicalOperation> operations;
}
