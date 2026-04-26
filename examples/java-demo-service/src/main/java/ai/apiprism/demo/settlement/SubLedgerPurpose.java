package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;

/** 子账本用途。 */
@Schema(description = "子账本用途")
public enum SubLedgerPurpose {
    @Schema(description = "可结算余额")
    SETTLEABLE,
    @Schema(description = "结算中（在途）")
    IN_TRANSIT,
    @Schema(description = "保证金")
    DEPOSIT,
    @Schema(description = "拒付准备金")
    CHARGEBACK_RESERVE,
    @Schema(description = "退款池")
    REFUND_POOL
}
