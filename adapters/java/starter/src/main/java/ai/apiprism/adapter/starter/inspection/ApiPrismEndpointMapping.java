package ai.apiprism.adapter.starter.inspection;

/**
 * 表示一个运行时或文档中的 API 端点映射（HTTP 方法 + 路径）。
 */
record ApiPrismEndpointMapping(String method, String path) {

    String display() {
        return method + " " + path;
    }
}
