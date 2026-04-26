package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;

/** 结算批次状态。 */
@Schema(description = "结算批次状态")
public enum SettlementStatus {
    @Schema(description = "已接收，待审批")
    PENDING_APPROVAL,
    @Schema(description = "出款中")
    PROCESSING,
    @Schema(description = "出款成功")
    SUCCEEDED,
    @Schema(description = "部分成功（部分行失败）")
    PARTIALLY_SUCCEEDED,
    @Schema(description = "整体失败")
    FAILED,
    @Schema(description = "已取消")
    CANCELED
}
