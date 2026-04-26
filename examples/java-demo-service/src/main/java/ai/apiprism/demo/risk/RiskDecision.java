package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;

/** 风控终判结果。 */
@Schema(description = "风控决策")
public enum RiskDecision {
    @Schema(description = "放行")
    ALLOW,
    @Schema(description = "需要二次验证")
    CHALLENGE,
    @Schema(description = "进入人工审核")
    REVIEW,
    @Schema(description = "拒绝")
    DENY
}
