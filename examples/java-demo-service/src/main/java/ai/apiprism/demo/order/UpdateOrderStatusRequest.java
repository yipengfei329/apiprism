package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 更新订单状态请求。 */
@Data
@Schema(description = "更新订单状态请求")
public class UpdateOrderStatusRequest {

    @Schema(description = "目标状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private OrderStatus targetStatus;

    @Schema(description = "操作原因，取消/拒绝时必填", example = "用户申请取消")
    private String reason;

    @Schema(description = "操作人账号（运营/客服）", example = "ops-staff-01")
    private String operatorId;
}
