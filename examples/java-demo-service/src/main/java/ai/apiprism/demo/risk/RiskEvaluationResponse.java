package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** 风险评估响应体（决策 + 评分 + 因子 + 命中规则 + 建议）。 */
@Data
@Builder
@Schema(description = "风险评估响应")
public class RiskEvaluationResponse {

    @Schema(description = "本次评估 ID（贯穿审计与回查）", example = "eval-2024-04-25-9001")
    private String evaluationId;

    @Schema(description = "原始请求 ID", example = "req-2024-04-25-9001")
    private String requestId;

    @Schema(description = "终判结果")
    private RiskDecision decision;

    @Schema(description = "风险评分（0-100，越大越危险）", example = "72.5")
    private Double riskScore;

    @Schema(description = "评分置信度（0-1）", example = "0.87")
    private Double confidence;

    @Schema(description = "贡献最大的若干风险因子（默认按 contribution 倒序）")
    private List<RiskFactor> factors;

    @Schema(description = "命中的规则清单")
    private List<RiskRuleHit> ruleHits;

    @Schema(description = "决策建议")
    private Recommendation recommendation;

    @Schema(description = "评估时间（ISO-8601）", example = "2024-04-25T15:32:11Z")
    private Instant evaluatedAt;

    @Schema(description = "服务侧总耗时（毫秒）", example = "87")
    private long latencyMs;

    @Schema(description = "数据来源标识（用于排错）", example = "model:v3.2#rules:rs-china-anti-fraud@2024-04-20")
    private String modelTrace;
}
