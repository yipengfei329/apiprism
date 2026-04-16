package ai.apiprism.protocol.registration;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * 适配器向中心注册 API 时提交的请求体。
 */
@Value
@Builder(toBuilder = true)
public class ApiRegistrationRequest {

    /**
     * 服务自身的身份与展示信息。
     */
    @NonNull
    ServiceDescriptor service;

    /**
     * 原始接口规格内容及其格式。
     */
    @NonNull
    SpecPayload spec;

    /**
     * 适配器或运行时附带的扩展元数据。
     */
    @Singular(value = "extension", ignoreNullCollections = true)
    Map<String, Object> extensions;
}
