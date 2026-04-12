package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** Facet 聚合中的单个桶。 */
@Data
@Builder
@Schema(description = "Facet 聚合桶")
public class FacetBucket {

    @Schema(description = "桶的 key（类目ID、品牌ID 或价格区间标签）", example = "brand-sony")
    private String key;

    @Schema(description = "展示标签", example = "Sony")
    private String label;

    @Schema(description = "命中数量", example = "42")
    private Long count;

    @Schema(description = "是否已被当前查询选中", example = "false")
    private Boolean selected;
}
