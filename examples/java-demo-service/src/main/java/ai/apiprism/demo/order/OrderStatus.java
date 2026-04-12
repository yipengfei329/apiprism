package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.media.Schema;

/** 订单状态枚举。 */
@Schema(description = "订单状态")
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDING,
    REFUNDED
}
