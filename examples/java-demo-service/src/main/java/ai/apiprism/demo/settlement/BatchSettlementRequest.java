package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 批量结算请求（结算行 + 出款指令 + 审批人）。 */
@Data
@Schema(description = "批量结算请求")
public class BatchSettlementRequest {

    @Schema(description = "调用方批次号（用于幂等）", example = "batch-2024-04-25-001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String batchId;

    @Schema(description = "商户号", example = "merchant-77001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String merchantId;

    @Schema(description = "结算周期", requiredMode = Schema.RequiredMode.REQUIRED)
    private SettlementCycle cycle;

    @Schema(description = "结算账期开始（含）", example = "2024-04-01")
    private java.time.LocalDate periodStart;

    @Schema(description = "结算账期结束（含）", example = "2024-04-25")
    private java.time.LocalDate periodEnd;

    @Schema(description = "结算行明细，每批最多 5000 条",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SettlementLine> lines;

    @Schema(description = "出款指令", requiredMode = Schema.RequiredMode.REQUIRED)
    private PayoutInstruction payoutInstruction;

    @Schema(description = "审批人账号列表（双签场景必填两人）",
            example = "[\"finance-001\",\"finance-002\"]")
    private List<String> approverUserIds;

    @Schema(description = "是否走仿真出款（沙箱环境推荐 true）", example = "false")
    private boolean dryRun;
}
