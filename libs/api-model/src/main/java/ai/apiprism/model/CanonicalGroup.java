package ai.apiprism.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * 归一化后按标签聚合出来的接口分组。
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class CanonicalGroup {

    /**
     * 分组名，通常来自首个 OpenAPI tag。
     */
    @NonNull
    String name;

    /**
     * URL 安全的分组标识符，由 name 自动生成。
     * ASCII 名称直接 slugify，CJK 等非 ASCII 名称先音译再 slugify。
     */
    String slug;

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
