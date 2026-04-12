package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 地理坐标（用于附近商品搜索）。 */
@Data
@Schema(description = "地理坐标")
public class GeoPoint {

    @Schema(description = "纬度，-90 ~ 90", example = "22.5431")
    private Double latitude;

    @Schema(description = "经度，-180 ~ 180", example = "114.0579")
    private Double longitude;

    @Schema(description = "搜索半径（千米）", example = "5.0")
    private Double radiusKm;
}
