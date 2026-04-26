package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** 商户余额快照（多币种 + 限额 + 近期动账）。 */
@Data
@Builder
@Schema(description = "商户余额快照")
public class MerchantBalanceSnapshot {

    @Schema(description = "商户号", example = "merchant-77001")
    private String merchantId;

    @Schema(description = "快照 ID", example = "snap-2024-04-25-9001")
    private String snapshotId;

    @Schema(description = "按币种分组的账户视图")
    private List<CurrencyAccount> accounts;

    @Schema(description = "出款限额")
    private PayoutLimits limits;

    @Schema(description = "最近 N 条动账（按时间倒序）")
    private List<RecentMovement> recentMovements;

    @Schema(description = "快照生成时间", example = "2024-04-25T16:30:00Z")
    private Instant generatedAt;
}
