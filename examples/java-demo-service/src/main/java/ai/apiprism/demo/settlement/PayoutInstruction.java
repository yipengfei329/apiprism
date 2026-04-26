package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/** 出款指令（结算批次的目标账户与合规字段）。 */
@Data
@Schema(description = "出款指令")
public class PayoutInstruction {

    @Schema(description = "出款渠道", requiredMode = Schema.RequiredMode.REQUIRED)
    private PayoutChannel channel;

    @Schema(description = "银行账户（channel = BANK_TRANSFER 时必填）")
    private BankAccount bankAccount;

    @Schema(description = "电子钱包标识（channel = E_WALLET 时必填）",
            example = "alipay:13800138000")
    private String walletId;

    @Schema(description = "稳定币地址（channel = STABLECOIN 时必填）",
            example = "0x4f3e1a8b...c92d")
    private String stablecoinAddress;

    @Schema(description = "稳定币网络", allowableValues = {"ETH", "TRON", "POLYGON"},
            example = "ETH")
    private String stablecoinNetwork;

    @Schema(description = "出款附言（28 字以内，部分通道会原样回填到对账单）",
            example = "Settlement-2024-04")
    private String memo;

    @Schema(description = "合规相关字段（按出款国/币种动态要求，如 PSP 牌照号、税号、申报码等）",
            example = "{\"tax_id\":\"91440300MA5G3X1234\",\"declaration_code\":\"100403\"}")
    private Map<String, String> regulatoryFields;
}
