package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** 出款限额视图（日/月维度）。 */
@Data
@Builder
@Schema(description = "出款限额")
public class PayoutLimits {

    @Schema(description = "日出款上限")
    private Money dailyLimit;

    @Schema(description = "今日已使用")
    private Money dailyUsed;

    @Schema(description = "月出款上限")
    private Money monthlyLimit;

    @Schema(description = "本月已使用")
    private Money monthlyUsed;

    @Schema(description = "单笔出款上限")
    private Money singleTransactionLimit;
}
