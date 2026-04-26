package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 单币种账户视图。 */
@Data
@Builder
@Schema(description = "币种账户")
public class CurrencyAccount {

    @Schema(description = "账户币种（ISO 4217）", example = "CNY")
    private String currency;

    @Schema(description = "可用余额")
    private Money available;

    @Schema(description = "在途余额（已发起出款但未到账）")
    private Money pending;

    @Schema(description = "冻结余额（拒付/争议保留）")
    private Money frozen;

    @Schema(description = "子账本拆分")
    private List<SubLedger> subLedgers;
}
