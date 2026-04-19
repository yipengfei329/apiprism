// 环境标签组件，用于面包屑和侧边栏统一展示环境信息

function getEnvThemeLight(env: string) {
  const lower = env.toLowerCase();
  if (lower === "production" || lower === "prod") {
    // prod 最重要，用唯一突出色紫色
    return { dot: "#7C3AED", bg: "rgba(124,58,237,0.06)", border: "rgba(124,58,237,0.20)", text: "#7C3AED" };
  }
  if (lower === "staging" || lower === "preview" || lower === "pre") {
    // staging 保留功能性琥珀色（警告信号）
    return { dot: "#F59E0B", bg: "#FFFBEB", border: "#FDE68A", text: "#B45309" };
  }
  if (lower === "test" || lower === "testing") {
    return { dot: "#666666", bg: "#F5F5F5", border: "#EBEBEB", text: "#444444" };
  }
  return { dot: "#999999", bg: "#F5F5F5", border: "#EBEBEB", text: "#666666" };
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
