package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;

/** 风险因子严重程度。 */
@Schema(description = "严重程度")
public enum Severity {
    @Schema(description = "信息")
    INFO,
    @Schema(description = "低危")
    LOW,
    @Schema(description = "中危")
    MEDIUM,
    @Schema(description = "高危")
    HIGH,
    @Schema(description = "严重")
    CRITICAL
}
