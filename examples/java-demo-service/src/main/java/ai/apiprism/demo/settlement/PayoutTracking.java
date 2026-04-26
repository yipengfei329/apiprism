package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 出款追踪信息。 */
@Data
@Builder
@Schema(description = "出款追踪")
public class PayoutTracking {

    @Schema(description = "出款 ID", example = "payout-2024-04-25-9001")
    private String payoutId;

    @Schema(description = "出款状态",
            allowableValues = {"INITIATED", "IN_FLIGHT", "POSTED", "RETURNED", "FAILED"},
            example = "POSTED")
    private String status;

    @Schema(description = "事件流（按时间正序）")
    private List<PayoutEvent> events;
}
