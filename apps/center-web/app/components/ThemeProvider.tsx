"use client";

import { ThemeProvider as NextThemesProvider } from "next-themes";
import { ReactNode } from "react";

/**
 * 主题 Provider：基于 next-themes 的客户端封装。
 * - attribute="class"：通过 <html class="dark"> 切换
 * - defaultTheme="system"：首次访问跟随系统
 * - disableTransitionOnChange：切换瞬间禁止过渡，避免颜色闪烁
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  return (
    <NextThemesProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
      storageKey="apiprism-theme"
    >
      {children}
    </NextThemesProvider>
  );
}
