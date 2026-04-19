"use client";

import Link from "next/link";

function getEnvTheme(env: string) {
  const lower = env.toLowerCase();
  if (lower === "production" || lower === "prod") {
    return {
      bg: "var(--env-prod-bg)",
      border: "var(--env-prod-border)",
      text: "var(--env-prod-text)",
      dot: "var(--env-prod-dot)",
    };
  }
  if (lower === "staging" || lower === "preview" || lower === "pre") {
    return {
      bg: "var(--env-staging-bg)",
      border: "var(--env-staging-border)",
      text: "var(--env-staging-text)",
      dot: "var(--env-staging-dot)",
    };
  }
  if (lower === "test" || lower === "testing") {
    return {
      bg: "var(--env-test-bg)",
      border: "var(--env-test-border)",
      text: "var(--env-test-text)",
      dot: "var(--env-test-dot)",
    };
  }
  return {
    bg: "var(--env-default-bg)",
    border: "var(--env-default-border)",
    text: "var(--env-default-text)",
    dot: "var(--env-default-dot)",
  };
}

interface EnvSwitcherProps {
  service: string;
  currentEnv: string;
  environments: string[];
}

export function EnvSwitcher({ service, currentEnv, environments }: EnvSwitcherProps) {
  if (environments.length <= 1) return null;

  return (
    <div
      role="tablist"
      aria-label="切换环境"
      className="flex flex-wrap gap-1.5"
    >
      {environments.map((env) => {
        const isActive = env === currentEnv;
        const theme = getEnvTheme(env);
        return (
          <Link
            key={env}
            href={`/docs/${encodeURIComponent(service)}/${encodeURIComponent(env)}`}
            role="tab"
            aria-selected={isActive}
            className="inline-flex items-center gap-1 rounded-full px-2.5 py-[3px] font-mono text-[11px] font-medium uppercase tracking-wider transition-colors"
            style={
              isActive
                ? { backgroundColor: theme.bg, border: `1px solid ${theme.border}`, color: theme.text }
                : { backgroundColor: "var(--bg-subtle)", border: "1px solid transparent", color: "var(--text-tertiary)" }
            }
          >
            {isActive && (
              <span
                className="inline-flex h-[5px] w-[5px] shrink-0 rounded-full"
                style={{ backgroundColor: theme.dot }}
              />
            )}
            {env}
          </Link>
        );
      })}
    </div>
  );
}
