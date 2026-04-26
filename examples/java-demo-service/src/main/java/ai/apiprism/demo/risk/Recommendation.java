package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** 建议的后续动作。 */
@Data
@Builder
@Schema(description = "决策建议")
public class Recommendation {

    @Schema(description = "建议执行的动作",
            allowableValues = {"PROCEED", "STEP_UP_AUTH", "HOLD_FOR_REVIEW", "BLOCK"},
            example = "STEP_UP_AUTH")
    private String action;

    @Schema(description = "需要的额外验证手段",
            example = "[\"SMS_OTP\",\"FACE_RECOGNITION\"]")
    private List<String> requiredVerifications;

    @Schema(description = "可选的替代支付渠道（当主渠道被建议拒绝时）",
            example = "[\"BANK_TRANSFER\"]")
    private List<String> alternativeChannels;

    @Schema(description = "建议有效期（ISO-8601），过期需重新评估",
            example = "2024-04-25T16:30:00Z")
    private Instant expiresAt;

    @Schema(description = "可选：转人工时附带的话术片段")
    private String reviewerNote;
}
