package ai.apiprism.center.mcp;

import ai.apiprism.center.config.CenterProperties;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP 端点配置 REST API：管理服务级和分组级 MCP 开关状态。
 */
@RestController
@RequestMapping("/api/v1/services/{service}/env/{environment}")
public class McpConfigController {

    private final McpEndpointRepository repository;
    private final CenterProperties centerProperties;

    public McpConfigController(McpEndpointRepository repository, CenterProperties centerProperties) {
        this.repository = repository;
        this.centerProperties = centerProperties;
    }

    /**
     * 获取服务的 MCP 状态：服务级开关 + 各分组开关。
     */
    @GetMapping("/mcp-config")
    public McpStatusResponse getStatus(
            @PathVariable String service,
            @PathVariable String environment
    ) {
        boolean serviceEnabled = repository.isServiceEnabled(service, environment);
        List<String> enabledGroups = repository.listEnabledGroups(service, environment);

        String baseUrl = resolveBaseUrl();
        String serviceEndpoint = baseUrl + "/mcp/" + service + "/" + environment;

        return new McpStatusResponse(
                serviceEnabled,
                serviceEnabled ? serviceEndpoint + "/sse" : null,
                serviceEnabled ? serviceEndpoint + "/mcp" : null,
                enabledGroups
        );
    }

    /**
     * 切换服务级 MCP 开关。
     */
    @PutMapping("/mcp-config")
    public McpToggleResponse toggleService(
            @PathVariable String service,
            @PathVariable String environment,
            @RequestBody McpToggleRequest request
    ) {
        repository.setServiceEnabled(service, environment, request.enabled());

        String baseUrl = resolveBaseUrl();
        String endpoint = baseUrl + "/mcp/" + service + "/" + environment;

        return new McpToggleResponse(
                request.enabled(),
                request.enabled() ? endpoint + "/sse" : null,
                request.enabled() ? endpoint + "/mcp" : null
        );
    }

    /**
     * 切换分组级 MCP 开关。
     */
    @PutMapping("/groups/{group}/mcp-config")
    public McpToggleResponse toggleGroup(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String group,
            @RequestBody McpToggleRequest request
    ) {
        repository.setGroupEnabled(service, environment, group, request.enabled());

        String baseUrl = resolveBaseUrl();
        String endpoint = baseUrl + "/mcp/" + service + "/" + environment + "/" + group;

        return new McpToggleResponse(
                request.enabled(),
                request.enabled() ? endpoint + "/sse" : null,
                request.enabled() ? endpoint + "/mcp" : null
        );
    }

    /**
     * 查询单个分组的 MCP 状态。
     */
    @GetMapping("/groups/{group}/mcp-config")
    public McpToggleResponse getGroupStatus(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String group
    ) {
        boolean enabled = repository.isGroupEnabled(service, environment, group);

        String baseUrl = resolveBaseUrl();
        String endpoint = baseUrl + "/mcp/" + service + "/" + environment + "/" + group;

        return new McpToggleResponse(
                enabled,
                enabled ? endpoint + "/sse" : null,
                enabled ? endpoint + "/mcp" : null
        );
    }

    private String resolveBaseUrl() {
        String external = centerProperties.getExternalUrl();
        if (external != null && !external.isBlank()) {
            return external.endsWith("/") ? external.substring(0, external.length() - 1) : external;
        }
        return "http://localhost:8080";
    }

    record McpToggleRequest(boolean enabled) {}

    record McpToggleResponse(boolean enabled, String sseEndpoint, String streamableEndpoint) {}

    record McpStatusResponse(
            boolean serviceEnabled,
            String sseEndpoint,
            String streamableEndpoint,
            List<String> enabledGroups
    ) {}
}
