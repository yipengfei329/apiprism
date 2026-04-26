package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 金额值对象（数值 + 货币）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "金额（数值 + ISO 货币代码）")
public class MoneyAmount {

    @Schema(description = "金额数值（最多 4 位小数）", example = "1299.50",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal value;

    @Schema(description = "ISO 4217 货币代码", example = "CNY",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;
}
