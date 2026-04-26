package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 单条结算行（对应一笔可结算订单或退款）。 */
@Data
@Schema(description = "结算行")
public class SettlementLine {

    @Schema(description = "业务单号（订单号 / 退款号）", example = "ORD-20240425-9001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderRef;

    @Schema(description = "业务类型",
            allowableValues = {"SALE", "REFUND", "CHARGEBACK", "ADJUSTMENT"},
            example = "SALE")
    private String businessType;

    @Schema(description = "原始成交金额（毛额）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Money grossAmount;

    @Schema(description = "费用拆分（佣金、通道费、汇兑费等）")
    private List<FeeBreakdown> fees;

    @Schema(description = "税费拆分")
    private List<TaxItem> taxes;

    @Schema(description = "汇兑信息（仅在跨币种结算时存在）")
    private FxConversion fxConversion;

    @Schema(description = "可结算净额", requiredMode = Schema.RequiredMode.REQUIRED)
    private Money netAmount;
}
