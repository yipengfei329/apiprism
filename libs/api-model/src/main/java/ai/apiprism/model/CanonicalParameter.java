package ai.apiprism.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

/**
 * 归一化后统一表达的请求参数。
 */
@Value
@Builder(toBuilder = true)
public class CanonicalParameter {

    /**
     * 参数名。
     */
    @NonNull
    String name;

    /**
     * 参数位置，例如 path、query、header。
     */
    @NonNull
    String location;

    /**
     * 参数是否必填。
     */
    boolean required;

    /**
     * 参数的 JSON Schema 定义。
     */
    Map<String, Object> schema;

    /**
     * 参数说明。
     */
    String description;
}
