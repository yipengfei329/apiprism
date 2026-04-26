package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;

/** 评估深度档位。 */
@Schema(description = "评估深度（影响延迟与计费）")
public enum EvaluationDepth {
    @Schema(description = "仅规则命中（毫秒级）")
    LIGHT,
    @Schema(description = "规则 + 模型评分（默认）")
    STANDARD,
    @Schema(description = "全量画像，含图谱关联与历史回溯（百毫秒级）")
    DEEP
}
