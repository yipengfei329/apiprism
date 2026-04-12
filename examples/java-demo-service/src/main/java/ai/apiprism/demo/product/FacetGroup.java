package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 一组 Facet 聚合结果。 */
@Data
@Builder
@Schema(description = "Facet 聚合组")
public class FacetGroup {

    @Schema(description = "聚合维度名", example = "brand")
    private String dimension;

    @Schema(description = "展示标题", example = "品牌")
    private String displayName;

    @Schema(description = "桶列表（按 count 降序）")
    private List<FacetBucket> buckets;
}
