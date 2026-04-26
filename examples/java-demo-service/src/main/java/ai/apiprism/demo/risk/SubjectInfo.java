package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** 评估主体身份信息。 */
@Data
@Schema(description = "评估主体身份信息")
public class SubjectInfo {

    @Schema(description = "主体唯一 ID", example = "user-abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "主体类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private SubjectType type;

    @Schema(description = "当前层级")
    private RiskTier tier;

    @Schema(description = "注册时间（ISO-8601）", example = "2023-05-21T10:00:00Z")
    private Instant registeredAt;

    @Schema(description = "已通过的 KYC 等级", example = "L2", allowableValues = {"L0", "L1", "L2", "L3"})
    private String kycLevel;

    @Schema(description = "归属国家代码（ISO 3166-1 alpha-2）", example = "CN")
    private String country;

    @Schema(description = "标签列表（运营/风控打标）", example = "[\"vip\",\"return_user\"]")
    private List<String> tags;
}
