package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 网络环境信息。 */
@Data
@Schema(description = "网络环境")
public class NetworkInfo {

    @Schema(description = "客户端公网 IP", example = "203.0.113.42")
    private String ip;

    @Schema(description = "IP 类型", allowableValues = {"IPV4", "IPV6"}, example = "IPV4")
    private String ipType;

    @Schema(description = "AS 号（自治域）", example = "4134")
    private Integer asn;

    @Schema(description = "ISP 名称", example = "China Telecom")
    private String isp;

    @Schema(description = "是否检测到 VPN", example = "false")
    private boolean vpnDetected;

    @Schema(description = "是否检测到代理", example = "false")
    private boolean proxyDetected;

    @Schema(description = "是否 Tor 出口节点", example = "false")
    private boolean torExitNode;

    @Schema(description = "网络往返时延（毫秒）", example = "42")
    private Integer rttMs;
}
