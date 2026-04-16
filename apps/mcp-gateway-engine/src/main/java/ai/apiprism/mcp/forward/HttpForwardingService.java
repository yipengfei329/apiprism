package ai.apiprism.mcp.forward;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalServiceSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 将 MCP 工具调用转发为 HTTP 请求到真实后端服务，并将响应转为 CallToolResult。
 */
public class HttpForwardingService {

    private static final Logger log = LoggerFactory.getLogger(HttpForwardingService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpForwardingService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 HTTP 转发：根据操作定义和工具参数构建 HTTP 请求，发送并返回结果。
     */
    public McpSchema.CallToolResult forward(
            CanonicalServiceSnapshot snapshot,
            CanonicalOperation operation,
            Map<String, Object> arguments
    ) {
        String baseUrl = resolveBaseUrl(snapshot);
        if (baseUrl == null) {
            return errorResult("No server URL configured for service: " + snapshot.getRef().getName());
        }

        try {
            String url = buildRequestUrl(baseUrl, operation, arguments);
            HttpMethod method = HttpMethod.valueOf(operation.getMethod().toUpperCase());

            log.info("Forwarding MCP tool call [{}] -> {} {}", operation.getOperationId(), method, url);

            RestClient.RequestBodySpec spec = restClient.method(method).uri(url);

            // 设置 header 参数
            setHeaders(spec, operation, arguments);

            // 设置请求体
            setBody(spec, operation, arguments);

            String responseBody = spec.retrieve().body(String.class);

            log.info("MCP tool call [{}] completed successfully", operation.getOperationId());
            return successResult(responseBody != null ? responseBody : "");

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("MCP tool call [{}] returned HTTP {}: {}",
                    operation.getOperationId(), e.getStatusCode(), e.getResponseBodyAsString());
            return errorResult("HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            log.error("MCP tool call [{}] failed: backend unreachable", operation.getOperationId(), e);
            return errorResult("Backend unreachable: " + e.getMessage());

        } catch (Exception e) {
            log.error("MCP tool call [{}] failed unexpectedly", operation.getOperationId(), e);
            return errorResult("Forwarding error: " + e.getMessage());
        }
    }

    /**
     * 构建完整的请求 URL：baseUrl + 路径模板替换 + query 参数拼接。
     */
    String buildRequestUrl(String baseUrl, CanonicalOperation operation, Map<String, Object> arguments) {
        String path = operation.getPath();

        // 替换路径模板参数
        if (operation.getParameters() != null) {
            for (CanonicalParameter param : operation.getParameters()) {
                if ("path".equals(param.getLocation()) && arguments.containsKey(param.getName())) {
                    String value = String.valueOf(arguments.get(param.getName()));
                    path = path.replace("{" + param.getName() + "}", urlEncode(value));
                }
            }
        }

        // 拼接 query 参数
        StringJoiner queryJoiner = new StringJoiner("&");
        if (operation.getParameters() != null) {
            for (CanonicalParameter param : operation.getParameters()) {
                if ("query".equals(param.getLocation()) && arguments.containsKey(param.getName())) {
                    Object value = arguments.get(param.getName());
                    if (value != null) {
                        queryJoiner.add(urlEncode(param.getName()) + "=" + urlEncode(String.valueOf(value)));
                    }
                }
            }
        }

        String fullUrl = baseUrl + path;
        if (queryJoiner.length() > 0) {
            fullUrl += "?" + queryJoiner;
        }
        return fullUrl;
    }

    private void setHeaders(RestClient.RequestBodySpec spec, CanonicalOperation operation, Map<String, Object> arguments) {
        if (operation.getParameters() == null) {
            return;
        }
        for (CanonicalParameter param : operation.getParameters()) {
            if ("header".equals(param.getLocation()) && arguments.containsKey(param.getName())) {
                spec.header(param.getName(), String.valueOf(arguments.get(param.getName())));
            }
        }
    }

    private void setBody(RestClient.RequestBodySpec spec, CanonicalOperation operation, Map<String, Object> arguments) {
        if (operation.getRequestBody() == null || !arguments.containsKey("requestBody")) {
            return;
        }
        Object body = arguments.get("requestBody");
        String contentType = operation.getRequestBody().getContentType();
        if (contentType != null) {
            spec.contentType(MediaType.parseMediaType(contentType));
        } else {
            spec.contentType(MediaType.APPLICATION_JSON);
        }

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            spec.body(bodyJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize request body", e);
        }
    }

    private String resolveBaseUrl(CanonicalServiceSnapshot snapshot) {
        List<String> urls = snapshot.getServerUrls();
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        String url = urls.get(0);
        // 移除尾部斜杠
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static McpSchema.CallToolResult successResult(String text) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(text)))
                .build();
    }

    private static McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(message)))
                .isError(true)
                .build();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
