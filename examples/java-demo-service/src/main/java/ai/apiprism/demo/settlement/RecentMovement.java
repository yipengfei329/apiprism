package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** 最近账户变动条目（用于快照中的最近动账速览）。 */
@Data
@Builder
@Schema(description = "最近账户变动")
public class RecentMovement {

    @Schema(description = "变动类型",
            allowableValues = {"CREDIT", "DEBIT", "HOLD", "RELEASE"},
            example = "CREDIT")
    private String type;

    @Schema(description = "变动金额")
    private Money amount;

    @Schema(description = "对手方标识", example = "merchant-77001")
    private String counterparty;

    @Schema(description = "业务单号", example = "ORD-20240425-9001")
    private String reference;

    @Schema(description = "发生时间", example = "2024-04-25T16:00:00Z")
    private Instant occurredAt;
}
