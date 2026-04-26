package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 命中的单条风控规则。 */
@Data
@Builder
@Schema(description = "规则命中详情")
public class RiskRuleHit {

    @Schema(description = "规则 ID", example = "rule-velocity-001")
    private String ruleId;

    @Schema(description = "规则可读名称", example = "5 分钟内同设备多账号下单")
    private String name;

    @Schema(description = "所属规则集", example = "rs-china-anti-fraud")
    private String ruleSetId;

    @Schema(description = "规则版本", example = "v3.2.1")
    private String version;

    @Schema(description = "权重（>=0）", example = "0.5")
    private Double weight;

    @Schema(description = "触发该规则的字段路径", example = "[\"device.deviceId\",\"transaction.amount.value\"]")
    private List<String> matchedFields;

    @Schema(description = "命中时的特征值快照（仅当 returnExplanation=true 时返回）",
            example = "{\"window\":\"5m\",\"distinctSubjects\":4}")
    private java.util.Map<String, Object> snapshot;
}
