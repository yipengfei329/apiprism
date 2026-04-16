-- MCP 端点启用状态表：记录服务级和分组级 MCP 开关状态。
-- group_slug 为空字符串表示服务级，非空表示分组级。
CREATE TABLE mcp_endpoints (
    service_name  VARCHAR(255)  NOT NULL,
    environment   VARCHAR(100)  NOT NULL,
    group_slug    VARCHAR(255)  NOT NULL DEFAULT '',
    enabled       BOOLEAN       NOT NULL DEFAULT FALSE,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_mcp_endpoints PRIMARY KEY (service_name, environment, group_slug)
);
