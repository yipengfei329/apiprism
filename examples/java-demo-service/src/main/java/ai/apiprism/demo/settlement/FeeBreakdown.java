package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 单条费用拆分（订单行内嵌套）。 */
@Data
@Schema(description = "费用拆分项")
public class FeeBreakdown {

    @Schema(description = "费用类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private FeeType type;

    @Schema(description = "费用金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private Money amount;

    @Schema(description = "费率（小数形式，0.006 表示 0.6%）", example = "0.006")
    private java.math.BigDecimal rate;

    @Schema(description = "计费描述", example = "支付宝官方通道 0.6% 费率")
    private String description;
}
