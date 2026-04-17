-- 数据模型规范化：把 service_snapshots 中混合的 blob 拆成服务/环境/标签/接口等独立实体；
-- 新增 content_localizations 以 (entity, field, locale, source) 为维度承载描述类内容，
-- 适配器提交与中心改写并存、按优先级读取，不污染结构 hash。

CREATE TABLE services (
    id              VARCHAR(13)   PRIMARY KEY,
    name            VARCHAR(255)  NOT NULL,
    default_locale  VARCHAR(35)   NOT NULL DEFAULT 'zh-CN',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_services_name UNIQUE (name)
);

CREATE TABLE environments (
    id              VARCHAR(13)   PRIMARY KEY,
    code            VARCHAR(100)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_environments_code UNIQUE (code)
);

CREATE TABLE service_deployments (
    id                  VARCHAR(13)   PRIMARY KEY,
    service_id          VARCHAR(13)   NOT NULL,
    environment_id      VARCHAR(13)   NOT NULL,
    version             VARCHAR(100),
    adapter_type        VARCHAR(100),
    spec_format         VARCHAR(50),
    server_urls_json    CLOB,
    spec_hash           VARCHAR(64)   NOT NULL,
    warnings_json       CLOB,
    extensions_json     CLOB,
    last_registered_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deploy_service FOREIGN KEY (service_id) REFERENCES services (id),
    CONSTRAINT fk_deploy_env FOREIGN KEY (environment_id) REFERENCES environments (id),
    CONSTRAINT uq_deploy_service_env UNIQUE (service_id, environment_id)
);

CREATE TABLE tags (
    id              VARCHAR(13)   PRIMARY KEY,
    service_id      VARCHAR(13)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    slug            VARCHAR(255)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tags_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE,
    CONSTRAINT uq_tags_service_name UNIQUE (service_id, name)
);

CREATE TABLE operations (
    id                       VARCHAR(13)   PRIMARY KEY,
    service_id               VARCHAR(13)   NOT NULL,
    operation_key            VARCHAR(500)  NOT NULL,
    method                   VARCHAR(16)   NOT NULL,
    path                     VARCHAR(500)  NOT NULL,
    definition_hash          VARCHAR(64)   NOT NULL,
    security_requirements_json CLOB,
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_operations_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE,
    CONSTRAINT uq_operations_service_key UNIQUE (service_id, operation_key)
);

CREATE INDEX idx_operations_service ON operations (service_id);

CREATE TABLE operation_tags (
    operation_id    VARCHAR(13)   NOT NULL,
    tag_id          VARCHAR(13)   NOT NULL,
    CONSTRAINT pk_operation_tags PRIMARY KEY (operation_id, tag_id),
    CONSTRAINT fk_optag_op FOREIGN KEY (operation_id) REFERENCES operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_optag_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE TABLE operation_parameters (
    id              VARCHAR(13)   PRIMARY KEY,
    operation_id    VARCHAR(13)   NOT NULL,
    location        VARCHAR(16)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    required        BOOLEAN       NOT NULL DEFAULT FALSE,
    schema_json     CLOB,
    sort_order      INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_opparam_op FOREIGN KEY (operation_id) REFERENCES operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_opparam_op ON operation_parameters (operation_id);

CREATE TABLE operation_request_bodies (
    id              VARCHAR(13)   PRIMARY KEY,
    operation_id    VARCHAR(13)   NOT NULL,
    content_type    VARCHAR(120),
    required        BOOLEAN       NOT NULL DEFAULT FALSE,
    schema_json     CLOB,
    CONSTRAINT fk_opbody_op FOREIGN KEY (operation_id) REFERENCES operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_opbody_op ON operation_request_bodies (operation_id);

CREATE TABLE operation_responses (
    id              VARCHAR(13)   PRIMARY KEY,
    operation_id    VARCHAR(13)   NOT NULL,
    status_code     VARCHAR(8)    NOT NULL,
    content_type    VARCHAR(120),
    schema_json     CLOB,
    CONSTRAINT fk_opresp_op FOREIGN KEY (operation_id) REFERENCES operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_opresp_op ON operation_responses (operation_id);

CREATE TABLE deployment_operations (
    deployment_id   VARCHAR(13)   NOT NULL,
    operation_id    VARCHAR(13)   NOT NULL,
    CONSTRAINT pk_deployment_operations PRIMARY KEY (deployment_id, operation_id),
    CONSTRAINT fk_depop_deploy FOREIGN KEY (deployment_id) REFERENCES service_deployments (id) ON DELETE CASCADE,
    CONSTRAINT fk_depop_op FOREIGN KEY (operation_id) REFERENCES operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_depop_deploy ON deployment_operations (deployment_id);

-- 接口定义版本历史：definition_hash 变化时追加一行；相同 hash 由 UNIQUE 拦截不重复写。
CREATE TABLE operation_definition_versions (
    id                    VARCHAR(13)   PRIMARY KEY,
    operation_id          VARCHAR(13)   NOT NULL,
    definition_hash       VARCHAR(64)   NOT NULL,
    definition_json       CLOB          NOT NULL,
    source_deployment_id  VARCHAR(13),
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_opver_op FOREIGN KEY (operation_id) REFERENCES operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_opver_deploy FOREIGN KEY (source_deployment_id) REFERENCES service_deployments (id) ON DELETE SET NULL,
    CONSTRAINT uq_opver_op_hash UNIQUE (operation_id, definition_hash)
);

CREATE INDEX idx_opver_op ON operation_definition_versions (operation_id);

-- 描述类内容本地化层。同一 (entity, field, locale) 下：
--   source='adapter' 行由适配器注册覆盖；source='center' 行由中心人工维护。
-- 读取时按 (userLocale,'center') > (userLocale,'adapter') > (defaultLocale,'center') > (defaultLocale,'adapter') 回落。
CREATE TABLE content_localizations (
    id              VARCHAR(13)   PRIMARY KEY,
    entity_type     VARCHAR(32)   NOT NULL,
    entity_id       VARCHAR(13)   NOT NULL,
    field           VARCHAR(48)   NOT NULL,
    locale          VARCHAR(35)   NOT NULL,
    source          VARCHAR(16)   NOT NULL,
    content         CLOB,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_loc_natural UNIQUE (entity_type, entity_id, field, locale, source),
    CONSTRAINT ck_loc_source CHECK (source IN ('adapter', 'center'))
);

CREATE INDEX idx_loc_lookup ON content_localizations (entity_type, entity_id, field);
