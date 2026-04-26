package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 人工复核提交请求体。 */
@Data
@Schema(description = "人工复核请求")
public class ManualReviewRequest {

    @Schema(description = "复核员账号", example = "reviewer-007",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reviewerId;

    @Schema(description = "最终决策", requiredMode = Schema.RequiredMode.REQUIRED)
    private RiskDecision finalDecision;

    @Schema(description = "决策理由（结构化分类）", example = "DOC_VERIFIED",
            allowableValues = {"DOC_VERIFIED", "USER_CONTACTED", "FALSE_POSITIVE",
                               "POLICY_OVERRIDE", "OTHER"})
    private String reasonCode;

    @Schema(description = "理由说明（自由文本）", example = "已电话核实身份，放行本次交易。")
    private String comment;

    @Schema(description = "处置后的额外限制（账号级，多个标签可叠加）",
            example = "[\"reduce_daily_limit\",\"watchlist\"]")
    private List<String> appliedRestrictions;

    @Schema(description = "上传的证据材料 ID 列表（已通过对象存储预签名）",
            example = "[\"obj-2024-04-25-001\",\"obj-2024-04-25-002\"]")
    private List<String> evidenceObjectIds;

    @Schema(description = "传入风控审计日志的扩展字段",
            example = "{\"escalated_from\":\"L1\"}")
    private Map<String, String> auditExtras;
}
