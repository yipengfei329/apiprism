package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** 人工复核处理结果。 */
@Data
@Builder
@Schema(description = "人工复核响应")
public class ManualReviewResponse {

    @Schema(description = "审核单号", example = "case-2024-04-25-0042")
    private String caseId;

    @Schema(description = "原始评估 ID", example = "eval-2024-04-25-9001")
    private String evaluationId;

    @Schema(description = "最终决策")
    private RiskDecision finalDecision;

    @Schema(description = "复核员账号", example = "reviewer-007")
    private String reviewerId;

    @Schema(description = "处理时间", example = "2024-04-25T16:00:00Z")
    private Instant decidedAt;

    @Schema(description = "处置耗时（秒）", example = "118")
    private long handlingSeconds;

    @Schema(description = "下游联动状态（钱包冻结/解冻、邮件通知等触发结果）",
            example = "WALLET_UNFROZEN,EMAIL_SENT")
    private String downstreamSignals;
}
