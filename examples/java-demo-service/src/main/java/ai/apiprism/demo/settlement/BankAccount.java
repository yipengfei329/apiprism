package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 银行账户信息。 */
@Data
@Schema(description = "银行账户信息")
public class BankAccount {

    @Schema(description = "户名", example = "深圳市某某科技有限公司",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String holderName;

    @Schema(description = "账号（含支付通道掩码）", example = "62258XXXXXX1234",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountNumber;

    @Schema(description = "银行名称", example = "招商银行深圳分行")
    private String bankName;

    @Schema(description = "SWIFT BIC（跨境必填）", example = "CMBCCNBS")
    private String swiftCode;

    @Schema(description = "IBAN（欧元区必填）", example = "DE89 3704 0044 0532 0130 00")
    private String iban;

    @Schema(description = "支行联行号", example = "308584000123")
    private String branchCode;

    @Schema(description = "账户币种", example = "CNY")
    private String currency;
}
