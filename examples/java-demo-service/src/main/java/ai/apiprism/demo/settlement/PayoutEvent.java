package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** 出款生命周期事件（用于跟踪银行流转/失败等节点）。 */
@Data
@Builder
@Schema(description = "出款事件")
public class PayoutEvent {

    @Schema(description = "事件发生时间", example = "2024-04-25T16:42:11Z")
    private Instant occurredAt;

    @Schema(description = "事件码",
            allowableValues = {"SUBMITTED", "ACCEPTED_BY_BANK", "RETURNED", "POSTED", "BOUNCED"},
            example = "ACCEPTED_BY_BANK")
    private String code;

    @Schema(description = "事件描述", example = "招商银行已受理")
    private String description;

    @Schema(description = "通道返回的原始引用号", example = "CMB-202404251642-9999")
    private String externalReference;
}
