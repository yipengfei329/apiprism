package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/** 评估请求的开关与定制参数。 */
@Data
@Schema(description = "评估选项")
public class EvaluationOptions {

    @Schema(description = "评估深度，缺省为 STANDARD")
    private EvaluationDepth depth;

    @Schema(description = "需要执行的规则集 ID，留空则使用默认规则集",
            example = "[\"rs-default\",\"rs-china-anti-fraud\"]")
    private Set<String> ruleSetIds;

    @Schema(description = "是否在响应中返回可解释性信息（命中规则、特征贡献度等）",
            example = "true")
    private boolean returnExplanation;

    @Schema(description = "是否同步写入风控审计日志", example = "true")
    private boolean writeAuditLog = true;

    @Schema(description = "决策结果的客户端缓存秒数（0 表示不缓存）", example = "60")
    private Integer cacheSeconds;

    @Schema(description = "降级策略：当评分服务不可用时的回退动作",
            allowableValues = {"FAIL_OPEN", "FAIL_CLOSED", "USE_LAST_GOOD"},
            example = "FAIL_CLOSED")
    private String fallbackStrategy;
}
