package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 设备指纹信号项。 */
@Data
@Schema(description = "设备指纹信号")
public class DeviceSignal {

    @Schema(description = "信号名称", example = "screen_resolution",
            allowableValues = {"screen_resolution", "battery_level", "sensor_count",
                               "touch_events", "webgl_vendor", "rooted", "emulator"})
    private String name;

    @Schema(description = "原始取值（字符串化）", example = "1920x1080")
    private String value;

    @Schema(description = "信号置信度（0-1）", example = "0.92")
    private Double confidence;

    @Schema(description = "归一化后的风险权重（-1 ~ 1，正数表示加分项）", example = "-0.15")
    private Double weight;
}
