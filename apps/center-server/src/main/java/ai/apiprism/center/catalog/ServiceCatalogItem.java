package ai.apiprism.center.catalog;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * 服务目录列表项，用于前端展示，包含服务基本信息和所属分组。
 */
@Getter
@Builder
public class ServiceCatalogItem {

    /** 服务名称 */
    private final String name;

    /** 部署环境标识 */
    private final String environment;

    /** 服务标题（来自 OpenAPI info.title） */
    private final String title;

    /** 服务版本（来自 OpenAPI info.version） */
    private final String version;

    /** 最近注册时间 */
    private final Instant updatedAt;

    /** 所属 API 分组摘要列表 */
    private final List<GroupRef> groups;

    /**
     * 分组摘要：名称 + URL slug。
     */
    @Getter
    @Builder
    public static class GroupRef {
        private final String name;
        private final String slug;
    }
}
