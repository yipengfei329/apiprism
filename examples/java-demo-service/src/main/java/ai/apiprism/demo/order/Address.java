package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 收货地址。 */
@Data
@Schema(description = "收货地址")
public class Address {

    @Schema(description = "收件人姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recipientName;

    @Schema(description = "手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "省份", example = "广东省")
    private String province;

    @Schema(description = "城市", example = "深圳市")
    private String city;

    @Schema(description = "区/县", example = "南山区")
    private String district;

    @Schema(description = "详细街道地址", example = "科技园南区8栋101")
    private String street;

    @Schema(description = "邮政编码", example = "518000")
    private String zipCode;
}
