package ai.apiprism.center.mcp;

import ai.apiprism.center.catalog.CatalogService;
import ai.apiprism.center.config.CenterProperties;
import ai.apiprism.model.CanonicalServiceSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * MCP 端点配置 REST API：管理服务级和分组级 MCP 开关状态。
 */
@RestController
@RequestMapping("/api/v1/services/{service}/{environment}")
public class McpConfigController {

    private static final Logger log = LoggerFactory.getLogger(McpConfigController.class);

    private final McpEndpointRepository repository;
    private final CatalogService catalogService;
    private final CenterProperties centerProperties;
    private final HttpClient httpClient;

    public McpConfigController(McpEndpointRepository repository,
                               CatalogService catalogService,
                               CenterProperties centerProperties) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.centerProperties = centerProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
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
        if (request.enabled()) {
            CanonicalServiceSnapshot snapshot = catalogService.getService(service, environment);
            validateServerUrls(snapshot);
            checkConnectivity(snapshot);
        }

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
    @PutMapping("/{group}/mcp-config")
    public McpToggleResponse toggleGroup(
            @PathVariable String service,
            @PathVariable String environment,
            @PathVariable String group,
            @RequestBody McpToggleRequest request
    ) {
        if (request.enabled()) {
            CanonicalServiceSnapshot snapshot = catalogService.getService(service, environment);
            validateServerUrls(snapshot);
            checkConnectivity(snapshot);
        }

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
    @GetMapping("/{group}/mcp-config")
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

    /**
     * 校验服务必须有 serverUrls，否则 MCP 转发无目标。
     */
    private void validateServerUrls(CanonicalServiceSnapshot snapshot) {
        if (snapshot.getServerUrls() == null || snapshot.getServerUrls().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "无法启用 MCP：该服务未配置 serverUrls，MCP 网关无转发目标地址。");
        }
    }

    /**
     * 尝试 HEAD 请求探测后端服务连通性，不可达时阻止启用。
     */
    private void checkConnectivity(CanonicalServiceSnapshot snapshot) {
        String targetUrl = snapshot.getServerUrls().get(0);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Connectivity check to {} returned HTTP {}", targetUrl, response.statusCode());
            // 任何 HTTP 响应（包括 4xx/5xx）都说明服务可达
        } catch (Exception e) {
            log.warn("Connectivity check to {} failed: {}", targetUrl, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "无法启用 MCP：后端服务不可达 (" + targetUrl + ")。请确认服务地址正确且服务已启动。");
        }
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
