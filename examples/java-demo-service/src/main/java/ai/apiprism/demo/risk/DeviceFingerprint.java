package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 设备指纹聚合结构。 */
@Data
@Schema(description = "设备指纹（含位置、网络、原始信号集合）")
public class DeviceFingerprint {

    @Schema(description = "设备唯一标识（IDFA / OAID / 自定义指纹）",
            example = "fp-2f8e6d3c-1b9a", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;

    @Schema(description = "设备类型", allowableValues = {"IOS", "ANDROID", "WEB", "MINI_PROGRAM"},
            example = "ANDROID")
    private String platform;

    @Schema(description = "操作系统版本", example = "Android 14")
    private String osVersion;

    @Schema(description = "User-Agent", example = "Mozilla/5.0 (Linux; Android 14)")
    private String userAgent;

    @Schema(description = "地理位置")
    private GeoLocation geoLocation;

    @Schema(description = "网络环境")
    private NetworkInfo network;

    @Schema(description = "原始设备信号集合")
    private List<DeviceSignal> signals;
}
