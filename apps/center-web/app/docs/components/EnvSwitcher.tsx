"use client";

import Link from "next/link";

function getEnvThemeLight(env: string) {
  const lower = env.toLowerCase();
  if (lower === "production" || lower === "prod") {
    return { bg: "#F0FDF4", border: "#BBF7D0", text: "#15803D", dot: "#22C55E" };
  }
  if (lower === "staging" || lower === "preview" || lower === "pre") {
    return { bg: "#FFFBEB", border: "#FDE68A", text: "#B45309", dot: "#F59E0B" };
  }
  if (lower === "test" || lower === "testing") {
    return { bg: "#F5F3FF", border: "#DDD6FE", text: "#6D28D9", dot: "#8B5CF6" };
  }
  return { bg: "#EFF6FF", border: "#BFDBFE", text: "#1D4ED8", dot: "#3B82F6" };
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
        const theme = getEnvThemeLight(env);
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
                : { backgroundColor: "#F4F4F5", border: "1px solid transparent", color: "#8E8E93" }
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
