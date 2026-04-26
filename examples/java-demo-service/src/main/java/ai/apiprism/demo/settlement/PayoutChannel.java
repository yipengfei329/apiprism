package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;

/** 出款渠道。 */
@Schema(description = "出款渠道")
public enum PayoutChannel {
    @Schema(description = "本地银行转账")
    BANK_TRANSFER,
    @Schema(description = "电子钱包")
    E_WALLET,
    @Schema(description = "虚拟账户")
    VIRTUAL_ACCOUNT,
    @Schema(description = "稳定币（USDC / USDT）")
    STABLECOIN
}
