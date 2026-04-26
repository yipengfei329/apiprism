package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/** 单项税费。 */
@Data
@Schema(description = "税费项")
public class TaxItem {

    @Schema(description = "税种", requiredMode = Schema.RequiredMode.REQUIRED)
    private TaxType type;

    @Schema(description = "适用税率（小数形式）", example = "0.13")
    private BigDecimal rate;

    @Schema(description = "税额", requiredMode = Schema.RequiredMode.REQUIRED)
    private Money amount;

    @Schema(description = "税务管辖地（ISO 3166-1 alpha-2）", example = "CN")
    private String jurisdiction;
}
