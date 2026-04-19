// 环境标签组件：使用主题可感知的 CSS 变量，light/dark 自动切换
// prod 使用 accent（teal），统一"最重要信号"

type EnvTokens = { dot: string; bg: string; border: string; text: string };

function getEnvTheme(env: string): EnvTokens {
  const lower = env.toLowerCase();
  if (lower === "production" || lower === "prod") {
    return {
      dot: "var(--env-prod-dot)",
      bg: "var(--env-prod-bg)",
      border: "var(--env-prod-border)",
      text: "var(--env-prod-text)",
    };
  }
  if (lower === "staging" || lower === "preview" || lower === "pre") {
    return {
      dot: "var(--env-staging-dot)",
      bg: "var(--env-staging-bg)",
      border: "var(--env-staging-border)",
      text: "var(--env-staging-text)",
    };
  }
  if (lower === "test" || lower === "testing") {
    return {
      dot: "var(--env-test-dot)",
      bg: "var(--env-test-bg)",
      border: "var(--env-test-border)",
      text: "var(--env-test-text)",
    };
  }
  return {
    dot: "var(--env-default-dot)",
    bg: "var(--env-default-bg)",
    border: "var(--env-default-border)",
    text: "var(--env-default-text)",
  };
}

export function EnvBadge({ env }: { env: string }) {
  const theme = getEnvTheme(env);

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
