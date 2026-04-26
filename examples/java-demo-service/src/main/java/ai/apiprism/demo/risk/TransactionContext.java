package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/** 交易上下文。 */
@Data
@Schema(description = "待评估的交易上下文")
public class TransactionContext {

    @Schema(description = "交易号", example = "TX-2024-04-25-9001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String transactionId;

    @Schema(description = "金额")
    private MoneyAmount amount;

    @Schema(description = "支付渠道", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentChannel channel;

    @Schema(description = "交易对手方")
    private Counterparty counterparty;

    @Schema(description = "交易发起时间（ISO-8601）", example = "2024-04-25T15:32:11Z")
    private Instant initiatedAt;

    @Schema(description = "业务场景标识", example = "checkout_v2")
    private String scene;

    @Schema(description = "业务系统传入的扩展字段（透传至风控决策日志）",
            example = "{\"campaign\":\"618_promo\",\"voucher\":\"SAVE20\"}")
    private Map<String, String> metadata;
}
