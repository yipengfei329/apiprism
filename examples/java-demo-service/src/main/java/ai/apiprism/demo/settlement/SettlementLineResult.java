package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** 结算单行处理结果（返回行级状态便于差错对账）。 */
@Data
@Builder
@Schema(description = "结算行处理结果")
public class SettlementLineResult {

    @Schema(description = "对应业务单号", example = "ORD-20240425-9001")
    private String orderRef;

    @Schema(description = "行状态", allowableValues = {"SUCCESS", "FAILED", "SKIPPED"},
            example = "SUCCESS")
    private String status;

    @Schema(description = "失败错误码（status = FAILED 时返回）", example = "ACCOUNT_FROZEN")
    private String errorCode;

    @Schema(description = "失败描述", example = "目标商户账户已被冻结")
    private String errorMessage;

    @Schema(description = "实际入账净额", example = "{\"value\":1290.50,\"currency\":\"CNY\"}")
    private Money settledAmount;
}
