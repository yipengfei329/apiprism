-- 从旧 service_snapshots 表回填到新规范化模型。
--
-- 设计权衡：service_snapshots 的 operation/group/schema 结构存在文件系统 normalized.json，
-- 不在 SQL 里。仅凭 SQL 可以回填 services / environments / service_deployments 的元数据，
-- 无法重建 operations 及其子表。
--
-- 策略：
--   1. 为每个不同的 service_name 建一条 services 记录（default_locale = 'zh-CN'）。
--   2. 为每个不同的 environment 建一条 environments 记录。
--   3. 为每条 (service_name, environment) 建一条 service_deployments 记录，spec_hash 故意
--      写入哨兵值 'backfilled-stale'，下次适配器注册时因 hash 不匹配会走完整结构解析路径，
--      重新把 operations / tags / children / localizations 填回来。
--   4. service_snapshots 表行保留到 V5 被 DROP，本迁移不再读旧文件；若文件丢失也不影响后续注册。
--
-- ID 生成：H2 RANDOM_UUID 截取前 13 位小写十六进制作为 VARCHAR(13) 主键，
-- 与 TSID 长度一致，业务无序但足以唯一。正常注册路径仍使用 TSID，
-- 只有这条一次性回填走这个变通方案。

INSERT INTO services (id, name, default_locale, created_at, updated_at)
SELECT LOWER(SUBSTRING(REPLACE(RANDOM_UUID(), '-', ''), 1, 13)), t.service_name,
       'zh-CN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM (SELECT DISTINCT service_name FROM service_snapshots) t
 WHERE NOT EXISTS (SELECT 1 FROM services s WHERE s.name = t.service_name);

INSERT INTO environments (id, code, created_at)
SELECT LOWER(SUBSTRING(REPLACE(RANDOM_UUID(), '-', ''), 1, 13)), t.environment, CURRENT_TIMESTAMP
  FROM (SELECT DISTINCT environment FROM service_snapshots) t
 WHERE NOT EXISTS (SELECT 1 FROM environments e WHERE e.code = t.environment);

INSERT INTO service_deployments (id, service_id, environment_id, version, adapter_type,
    spec_format, server_urls_json, spec_hash, warnings_json, extensions_json, last_registered_at)
SELECT LOWER(SUBSTRING(REPLACE(RANDOM_UUID(), '-', ''), 1, 13)),
       s.id, e.id, ss.version, ss.adapter_type, ss.spec_format,
       NULL, 'backfilled-stale', ss.warnings, ss.extensions, ss.registered_at
  FROM service_snapshots ss
  JOIN services     s ON s.name = ss.service_name
  JOIN environments e ON e.code = ss.environment
 WHERE NOT EXISTS (
        SELECT 1 FROM service_deployments d
         WHERE d.service_id = s.id AND d.environment_id = e.id
 );
