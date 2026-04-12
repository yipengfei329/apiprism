package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** SKU 实时库存与价格快照。 */
@Data
@Builder
@Schema(description = "SKU 实时快照")
public class SkuSnapshot {

    @Schema(description = "SKU ID", example = "SKU-20240101-001")
    private String skuId;

    @Schema(description = "当前售价（元）", example = "299.00")
    private BigDecimal currentPrice;

    @Schema(description = "当前库存数量", example = "100")
    private Integer stockQuantity;

    @Schema(description = "是否可购买（库存 > 0 且上架状态）", example = "true")
    private Boolean available;
}
