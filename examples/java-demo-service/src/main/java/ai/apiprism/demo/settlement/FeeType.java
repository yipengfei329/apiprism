package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;

/** 费用类型。 */
@Schema(description = "费用类型")
public enum FeeType {
    @Schema(description = "平台佣金")
    PLATFORM_COMMISSION,
    @Schema(description = "支付通道费")
    PAYMENT_GATEWAY,
    @Schema(description = "汇兑费")
    FX_FEE,
    @Schema(description = "退款手续费")
    REFUND_FEE,
    @Schema(description = "提现手续费")
    PAYOUT_FEE,
    @Schema(description = "杂项")
    MISC
}
