package ai.apiprism.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * 归一化后的单个接口操作定义。
 */
@Value
@Builder(toBuilder = true)
public class CanonicalOperation {

    /**
     * 操作的稳定标识，优先使用 OpenAPI 的 operationId。
     */
    @NonNull
    String operationId;

    /**
     * HTTP 方法。
     */
    @NonNull
    String method;

    /**
     * 请求路径模板。
     */
    @NonNull
    String path;

    /**
     * 简短摘要。
     */
    String summary;

    /**
     * 详细说明。
     */
    String description;

    /**
     * 原始规格中的标签列表。
     */
    @Singular(value = "tag", ignoreNullCollections = true)
    List<String> tags;

    /**
     * 归一化后保留下来的安全需求名称列表。
     */
    @Singular(value = "securityRequirement", ignoreNullCollections = true)
    List<String> securityRequirements;

    /**
     * 操作级参数列表。
     */
    @Singular(value = "parameter", ignoreNullCollections = true)
    List<CanonicalParameter> parameters;

    /**
     * 请求体摘要，没有时为 null。
     */
    CanonicalRequestBody requestBody;

    /**
     * 响应摘要列表。
     */
    @Singular(value = "response", ignoreNullCollections = true)
    List<CanonicalResponse> responses;
}
