package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 交易对手方信息。 */
@Data
@Schema(description = "交易对手方信息")
public class Counterparty {

    @Schema(description = "对手方 ID（商户号 / 收款账户标识）", example = "merchant-77001")
    private String id;

    @Schema(description = "类型", allowableValues = {"MERCHANT", "INDIVIDUAL", "PLATFORM"},
            example = "MERCHANT")
    private String type;

    @Schema(description = "已通过的 KYC 等级", example = "L3", allowableValues = {"L0", "L1", "L2", "L3"})
    private String kycLevel;

    @Schema(description = "归属国家代码（ISO 3166-1 alpha-2）", example = "SG")
    private String country;

    @Schema(description = "账户开通天数", example = "365")
    private Integer accountAgeDays;

    @Schema(description = "近 30 天交易笔数", example = "120")
    private Integer recentTxCount;
}
