// 环境标签组件，用于面包屑和侧边栏统一展示环境信息

/**
 * 环境色映射（亮色主题版本，用于白底页面）
 */
function getEnvThemeLight(env: string) {
  const lower = env.toLowerCase();
  if (lower === "production" || lower === "prod") {
    return { dot: "#22C55E", bg: "#F0FDF4", border: "#BBF7D0", text: "#15803D" };
  }
  if (lower === "staging" || lower === "preview" || lower === "pre") {
    return { dot: "#F59E0B", bg: "#FFFBEB", border: "#FDE68A", text: "#B45309" };
  }
  if (lower === "test" || lower === "testing") {
    return { dot: "#8B5CF6", bg: "#F5F3FF", border: "#DDD6FE", text: "#6D28D9" };
  }
  return { dot: "#3B82F6", bg: "#EFF6FF", border: "#BFDBFE", text: "#1D4ED8" };
}

export function EnvBadge({ env }: { env: string }) {
  const theme = getEnvThemeLight(env);

  return (
    <span
      className="inline-flex items-center gap-1 rounded-full px-2 py-[2px] font-mono text-[10px] font-medium uppercase tracking-wider"
      style={{
        backgroundColor: theme.bg,
        border: `1px solid ${theme.border}`,
        color: theme.text,
      }}
    >
      <span
        className="inline-flex h-[5px] w-[5px] shrink-0 rounded-full"
        style={{ backgroundColor: theme.dot }}
      />
      {env}
    </span>
  );
}
