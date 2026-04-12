package ai.apiprism.protocol.registration;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * 注册请求中携带的原始接口规格内容。
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class SpecPayload {

    /**
     * 规格格式标识，例如 openapi-json、openapi-yaml。
     */
    @NonNull
    String format;

    /**
     * 原始规格正文。
     */
    @NonNull
    String content;
}
