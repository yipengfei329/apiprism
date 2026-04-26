package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 结算域内部使用的金额值对象。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "金额（数值 + ISO 货币代码）")
public class Money {

    @Schema(description = "数值（最多 4 位小数）", example = "1299.5000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal value;

    @Schema(description = "ISO 4217 货币代码", example = "CNY",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;
}
