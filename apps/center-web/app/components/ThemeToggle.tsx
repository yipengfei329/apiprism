"use client";

import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import { Monitor, Sun, Moon } from "@phosphor-icons/react";

type Variant = "sidebar" | "surface";

/**
 * 主题切换按钮：三态循环 system → light → dark → system。
 *
 * - variant="sidebar"：渲染在常暗侧边栏内，使用 sidebar token
 * - variant="surface"：渲染在主区，使用主题色 token
 */
export function ThemeToggle({ variant = "surface" }: { variant?: Variant }) {
  const [mounted, setMounted] = useState(false);
  const { theme, setTheme } = useTheme();

  // 挂载前返回占位，避免水合不一致
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  const current = mounted ? theme ?? "system" : "system";

  const handleCycle = () => {
    const next =
      current === "system" ? "light" : current === "light" ? "dark" : "system";
    setTheme(next);
  };

  const Icon = current === "system" ? Monitor : current === "light" ? Sun : Moon;
  const label =
    current === "system"
      ? "跟随系统"
      : current === "light"
        ? "浅色主题"
        : "深色主题";

  const sidebarClasses =
    "text-[var(--sidebar-text-secondary)] hover:bg-[var(--sidebar-hover-bg)] hover:text-[var(--sidebar-text-primary)]";
  const surfaceClasses =
    "text-[var(--text-tertiary)] hover:bg-[var(--bg-subtle)] hover:text-[var(--text-primary)]";

  if (!mounted) {
    // 占位：同尺寸、无图标、不可点击，纯粹留位
    return (
      <div
        className="h-7 w-7 shrink-0"
        aria-hidden
      />
    );
  }

  return (
    <button
      type="button"
      onClick={handleCycle}
      aria-label={`切换主题（当前：${label}）`}
      title={`主题：${label}（点击切换）`}
      className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-md transition-colors duration-150 cursor-pointer ${
        variant === "sidebar" ? sidebarClasses : surfaceClasses
      }`}
    >
      <Icon size={15} weight="regular" />
    </button>
  );
}
