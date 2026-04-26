package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;

/** 主体所处的风险层级。 */
@Schema(description = "主体风险层级")
public enum RiskTier {
    @Schema(description = "低风险（白名单或长期良好行为）")
    LOW,
    @Schema(description = "标准层级")
    STANDARD,
    @Schema(description = "高风险（曾命中风控规则）")
    HIGH,
    @Schema(description = "黑名单")
    BLACKLISTED
}
