package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** 单条风险因子（特征级别的命中说明）。 */
@Data
@Builder
@Schema(description = "风险因子")
public class RiskFactor {

    @Schema(description = "因子编码", example = "device.proxy_detected")
    private String code;

    @Schema(description = "因子可读名称", example = "代理网络")
    private String label;

    @Schema(description = "严重程度")
    private Severity severity;

    @Schema(description = "对最终评分的贡献度（-1 ~ 1，正数代表加风险）", example = "0.32")
    private Double contribution;

    @Schema(description = "可解释描述", example = "本次请求来源 IP 命中 30 天内的代理库")
    private String description;

    @Schema(description = "证据字段（特征值快照）", example = "{\"ip\":\"203.0.113.42\",\"asn\":4134}")
    private java.util.Map<String, Object> evidence;
}
