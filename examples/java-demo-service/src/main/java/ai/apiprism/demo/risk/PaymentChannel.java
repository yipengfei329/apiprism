package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;

/** 支付渠道。 */
@Schema(description = "支付渠道")
public enum PaymentChannel {
    @Schema(description = "信用卡")
    CREDIT_CARD,
    @Schema(description = "借记卡")
    DEBIT_CARD,
    @Schema(description = "支付宝")
    ALIPAY,
    @Schema(description = "微信支付")
    WECHAT_PAY,
    @Schema(description = "银行转账")
    BANK_TRANSFER,
    @Schema(description = "数字钱包")
    DIGITAL_WALLET,
    @Schema(description = "加密资产")
    CRYPTO
}
