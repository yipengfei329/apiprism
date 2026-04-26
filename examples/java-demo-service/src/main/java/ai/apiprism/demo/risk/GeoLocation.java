package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 地理位置坐标 + 行政区划信息。 */
@Data
@Schema(description = "地理位置")
public class GeoLocation {

    @Schema(description = "纬度", example = "22.5431")
    private Double latitude;

    @Schema(description = "经度", example = "114.0579")
    private Double longitude;

    @Schema(description = "定位精度（米）", example = "35.0")
    private Double accuracyMeters;

    @Schema(description = "国家代码（ISO 3166-1 alpha-2）", example = "CN")
    private String country;

    @Schema(description = "省/州", example = "广东省")
    private String region;

    @Schema(description = "城市", example = "深圳市")
    private String city;

    @Schema(description = "时区", example = "Asia/Shanghai")
    private String timezone;
}
