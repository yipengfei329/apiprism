package ai.apiprism.demo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 账户摘要（嵌套在用户响应中）。 */
@Data
@Builder
@Schema(description = "账户资产摘要")
public class AccountSummary {

    @Schema(description = "账户余额（元）", example = "128.50")
    private BigDecimal balance;

    @Schema(description = "积分", example = "2500")
    private Long points;

    @Schema(description = "优惠券数量", example = "3")
    private Integer couponCount;

    @Schema(description = "会员等级", example = "GOLD",
            allowableValues = {"REGULAR", "SILVER", "GOLD", "PLATINUM", "DIAMOND"})
    private String memberLevel;

    @Schema(description = "累计消费金额（元）", example = "12800.00")
    private BigDecimal totalSpent;
}
