package ai.apiprism.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * 归一化后保留下来的请求体定义。
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class CanonicalRequestBody {

    /**
     * 请求体是否必填。
     */
    boolean required;

    /**
     * 首选内容类型。
     */
    String contentType;

    /**
     * 请求体的 JSON Schema 定义。
     */
    Map<String, Object> schema;
}
