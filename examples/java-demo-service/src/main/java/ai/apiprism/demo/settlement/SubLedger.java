package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** 子账本余额项。 */
@Data
@Builder
@Schema(description = "子账本余额")
public class SubLedger {

    @Schema(description = "用途")
    private SubLedgerPurpose purpose;

    @Schema(description = "余额")
    private Money balance;

    @Schema(description = "最近一次变动时间", example = "2024-04-25T15:55:00Z")
    private Instant lastUpdatedAt;
}
