package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/** 订单行项目。 */
@Data
@Schema(description = "订单行项目")
public class OrderItem {

    @Schema(description = "商品 SKU", example = "SKU-20240101-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String skuId;

    @Schema(description = "商品名称", example = "无线蓝牙耳机 Pro")
    private String productName;

    @Schema(description = "购买数量", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "单价（元）", example = "299.00")
    private BigDecimal unitPrice;

    @Schema(description = "折扣率，0.0-1.0，1.0 表示无折扣", example = "0.9")
    private Double discountRate;

    @Schema(description = "备注，例如颜色/尺寸偏好", example = "颜色：星空黑")
    private String remark;
}
