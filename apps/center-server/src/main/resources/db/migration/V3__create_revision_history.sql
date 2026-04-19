-- 服务注册版本历史表：每一次 spec hash 变化都作为一条历史 revision 追加。
-- service_snapshots 退化为 current pointer（每个 service+env 一行），所有读路径仍走该表，保持向后兼容。
CREATE TABLE service_snapshot_revisions (
    id                   VARCHAR(13)  PRIMARY KEY,
    service_name         VARCHAR(255) NOT NULL,
    environment          VARCHAR(100) NOT NULL,
    revision_seq         BIGINT       NOT NULL,
    spec_hash            VARCHAR(64)  NOT NULL,
    title                VARCHAR(500),
    version              VARCHAR(100),
    adapter_type         VARCHAR(100),
    spec_format          VARCHAR(50),
    warnings             CLOB,
    extensions           CLOB,
    source               VARCHAR(20)  NOT NULL DEFAULT 'REGISTER',
    previous_revision_id VARCHAR(13),
    registered_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rev_seq UNIQUE (service_name, environment, revision_seq)
);

CREATE INDEX idx_rev_svc_env ON service_snapshot_revisions(service_name, environment, registered_at DESC);

-- current pointer 列：指向 service_snapshot_revisions.id
ALTER TABLE service_snapshots ADD COLUMN current_revision_id VARCHAR(13);
-- 最近一次 adapter 注册时间（即使 spec 未变也刷新），用于观察 adapter 健康
ALTER TABLE service_snapshots ADD COLUMN last_seen_at TIMESTAMP;

-- 种子数据：把现存每一行 service_snapshots 作为 revision #1 写入历史表
INSERT INTO service_snapshot_revisions
 (id, service_name, environment, revision_seq, spec_hash, title, version,
  adapter_type, spec_format, warnings, extensions, source, registered_at)
SELECT id, service_name, environment, 1, spec_hash, title, version,
       adapter_type, spec_format, warnings, extensions, 'REGISTER', registered_at
  FROM service_snapshots;

UPDATE service_snapshots SET current_revision_id = id, last_seen_at = registered_at;
