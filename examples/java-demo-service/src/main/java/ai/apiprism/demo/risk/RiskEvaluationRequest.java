package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 风险评估请求体，含主体、设备、交易、行为序列与评估开关。 */
@Data
@Schema(description = "风险评估请求")
public class RiskEvaluationRequest {

    @Schema(description = "调用方业务请求 ID（用于幂等）",
            example = "req-2024-04-25-9001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requestId;

    @Schema(description = "租户编码", example = "tenant-merchant-77",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantId;

    @Schema(description = "评估主体", requiredMode = Schema.RequiredMode.REQUIRED)
    private SubjectInfo subject;

    @Schema(description = "设备指纹")
    private DeviceFingerprint device;

    @Schema(description = "待评估交易上下文（订单/转账/提现等）")
    private TransactionContext transaction;

    @Schema(description = "近期行为事件序列，按时间倒序，最多 50 条")
    private List<BehaviorEvent> recentBehavior;

    @Schema(description = "评估选项")
    private EvaluationOptions options;
}
