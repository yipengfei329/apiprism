package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;

/** 用户行为事件类型。 */
@Schema(description = "行为事件类型")
public enum BehaviorEventType {
    @Schema(description = "登录")
    LOGIN,
    @Schema(description = "登录失败")
    LOGIN_FAILED,
    @Schema(description = "修改密码")
    PASSWORD_CHANGED,
    @Schema(description = "更换设备")
    DEVICE_SWITCHED,
    @Schema(description = "新增收货地址")
    ADDRESS_ADDED,
    @Schema(description = "下单")
    ORDER_PLACED,
    @Schema(description = "申请退款")
    REFUND_REQUESTED,
    @Schema(description = "举报/投诉")
    COMPLAINT
}
