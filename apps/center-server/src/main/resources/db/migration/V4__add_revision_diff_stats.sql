-- 为 revision 历史表增加接口统计和变更差异列
-- endpoint_count: 该 revision 的接口总数
-- diff_stats: 与前驱 revision 相比的变更详情，JSON 格式
--   {"added":[{"method","path","operationId","summary"}],"removed":[...],"modified":[...]}
-- 旧 revision 两列均为 NULL，不做回填
ALTER TABLE service_snapshot_revisions ADD COLUMN endpoint_count INT;
ALTER TABLE service_snapshot_revisions ADD COLUMN diff_stats CLOB;
