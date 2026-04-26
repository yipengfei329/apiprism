package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;

/** 结算周期。 */
@Schema(description = "结算周期")
public enum SettlementCycle {
    @Schema(description = "T+0 即时结算")
    INSTANT,
    @Schema(description = "每日结算")
    DAILY,
    @Schema(description = "每周结算")
    WEEKLY,
    @Schema(description = "每月结算")
    MONTHLY,
    @Schema(description = "按需手动触发")
    ON_DEMAND
}
