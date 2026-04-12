package ai.apiprism.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

/**
 * 归一化后保留下来的响应定义。
 */
@Value
@Builder(toBuilder = true)
public class CanonicalResponse {

    /**
     * 响应状态码。
     */
    @NonNull
    String statusCode;

    /**
     * 响应说明。
     */
    String description;

    /**
     * 首选内容类型。
     */
    String contentType;

    /**
     * 响应体的 JSON Schema 定义。
     */
    Map<String, Object> schema;
}
