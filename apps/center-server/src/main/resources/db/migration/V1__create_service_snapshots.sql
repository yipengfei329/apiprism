CREATE TABLE service_snapshots (
    id              VARCHAR(13)   PRIMARY KEY,
    service_name    VARCHAR(255)  NOT NULL,
    environment     VARCHAR(100)  NOT NULL,
    title           VARCHAR(500),
    version         VARCHAR(100),
    adapter_type    VARCHAR(100),
    spec_format     VARCHAR(50),
    spec_hash       VARCHAR(64)   NOT NULL,
    warnings        CLOB,
    extensions      CLOB,
    registered_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_service_env UNIQUE (service_name, environment)
);
