package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/** 汇兑信息。 */
@Data
@Schema(description = "汇兑信息（跨币种结算）")
public class FxConversion {

    @Schema(description = "源币种（ISO 4217）", example = "USD",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceCurrency;

    @Schema(description = "目标币种（ISO 4217）", example = "CNY",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetCurrency;

    @Schema(description = "汇率（1 单位源币 = X 单位目标币）", example = "7.2345")
    private BigDecimal rate;

    @Schema(description = "汇率提供方", example = "PBOC", allowableValues = {"PBOC", "ECB", "INTERNAL"})
    private String provider;

    @Schema(description = "汇率确定时间", example = "2024-04-25T09:30:00Z")
    private Instant quotedAt;
}
