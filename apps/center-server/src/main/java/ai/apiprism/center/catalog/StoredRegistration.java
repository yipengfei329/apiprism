package ai.apiprism.center.catalog;

import ai.apiprism.model.CanonicalServiceSnapshot;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 存储在注册中心内存库中的服务注册快照，包含原始规范、规范化模型、警告及扩展信息。
 */
@Getter
@Builder
public class StoredRegistration {

    /** 注册记录唯一 ID */
    private final String id;

    /** 原始 OpenAPI 规范内容 */
    private final String rawSpec;

    /** 规范格式标识（如 "openapi-json"、"openapi-yaml"） */
    private final String specFormat;

    /** 产生本次注册的适配器类型（如 "spring-boot-starter"） */
    private final String adapterType;

    /** 规范化后的服务快照 */
    private final CanonicalServiceSnapshot snapshot;

    /** 规范化过程产生的警告列表 */
    private final List<String> warnings;

    /** 来自适配器的扩展字段 */
    private final Map<String, Object> extensions;
}
