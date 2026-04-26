package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;

/** 风险评估的主体类型。 */
@Schema(description = "评估主体类型")
public enum SubjectType {
    @Schema(description = "终端用户")
    END_USER,
    @Schema(description = "商户")
    MERCHANT,
    @Schema(description = "设备")
    DEVICE,
    @Schema(description = "账号")
    ACCOUNT
}
