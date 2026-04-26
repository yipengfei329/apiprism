package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/** 单条用户行为事件，用于刻画近期行为序列。 */
@Data
@Schema(description = "用户行为事件")
public class BehaviorEvent {

    @Schema(description = "事件类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private BehaviorEventType type;

    @Schema(description = "事件发生时间（ISO-8601）", example = "2024-04-25T15:30:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant occurredAt;

    @Schema(description = "事件来源（IP/Device/系统等）", example = "DEVICE")
    private String source;

    @Schema(description = "命中规则数（与该事件关联）", example = "0")
    private Integer ruleHits;

    @Schema(description = "事件附加属性", example = "{\"channel\":\"app\"}")
    private Map<String, String> attributes;
}
