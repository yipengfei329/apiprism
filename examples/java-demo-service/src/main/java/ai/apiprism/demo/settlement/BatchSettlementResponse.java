package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** 批量结算响应。 */
@Data
@Builder
@Schema(description = "批量结算响应")
public class BatchSettlementResponse {

    @Schema(description = "调用方批次号", example = "batch-2024-04-25-001")
    private String batchId;

    @Schema(description = "系统结算 ID", example = "settle-2024-04-25-9001")
    private String settlementId;

    @Schema(description = "整体状态")
    private SettlementStatus status;

    @Schema(description = "原始毛额合计")
    private Money totalGross;

    @Schema(description = "费用合计")
    private Money totalFees;

    @Schema(description = "实结净额合计")
    private Money totalNet;

    @Schema(description = "行级处理结果")
    private List<SettlementLineResult> lineResults;

    @Schema(description = "出款追踪")
    private PayoutTracking payoutTracking;

    @Schema(description = "提交时间", example = "2024-04-25T16:00:00Z")
    private Instant submittedAt;

    @Schema(description = "完成时间（终态）", example = "2024-04-25T16:42:11Z")
    private Instant completedAt;
}
