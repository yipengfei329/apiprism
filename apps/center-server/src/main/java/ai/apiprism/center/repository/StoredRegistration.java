package ai.apiprism.center.repository;

import ai.apiprism.model.CanonicalServiceSnapshot;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 存储在注册中心的服务注册快照，包含原始规范、规范化模型、警告及扩展信息。
 * 同时承载版本历史相关的元数据（序号、来源、是否 current）。
 */
@Getter
@Builder(toBuilder = true)
public class StoredRegistration {

    /** 注册记录唯一 ID，同时作为 FileSpecStore 的文件名和历史 revision 的主键。 */
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

    /** 原始规范内容的 SHA-256 哈希，用于快速判断规范是否变化 */
    private final String specHash;

    /** 本记录在 (service, env) 维度下的版本序号，从 1 开始。 */
    private final Long revisionSeq;

    /** 本记录的来源，REGISTER 表示 adapter 正常注册追加，预留 IMPORT 等扩展值。 */
    private final String source;

    /** 本记录是否为当前 current pointer 所指向的 revision。 */
    private final boolean current;

    /** 本 revision 注册入库的时间。 */
    private final Instant registeredAt;
}
