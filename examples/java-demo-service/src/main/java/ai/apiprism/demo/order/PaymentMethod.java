package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.media.Schema;

/** 支付方式枚举。 */
@Schema(description = "支付方式")
public enum PaymentMethod {
    ALIPAY,
    WECHAT_PAY,
    CREDIT_CARD,
    DEBIT_CARD,
    COD
}
