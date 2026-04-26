"use client";

import { useState, useEffect, useCallback, ReactNode, Suspense } from "react";
import { usePathname } from "next/navigation";
import { ServiceCatalogItem } from "../lib/api";
import { DocsSidebar } from "./DocsSidebar";
import { SidebarSimple, List } from "@phosphor-icons/react";

const MD_BREAKPOINT = 768;

export function SidebarLayout({
  services,
  children,
}: {
  services: ServiceCatalogItem[];
  children: ReactNode;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  // 首次渲染固定为桌面结构，hydrate 后再同步真实断点，避免 SSR/CSR 树不一致
  const [isDesktop, setIsDesktop] = useState(true);
  const pathname = usePathname();

  // 监听视口断点变化
  useEffect(() => {
    const mq = window.matchMedia(`(min-width: ${MD_BREAKPOINT}px)`);
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsDesktop(mq.matches);
    const handler = (e: MediaQueryListEvent) => {
      setIsDesktop(e.matches);
      if (e.matches) setMobileOpen(false);
    };
    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, []);

  // 移动端路由切换时自动收起侧边栏
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { setMobileOpen(false); }, [pathname]);

  const handleCollapse = useCallback(() => {
    if (isDesktop) {
      setCollapsed(true);
    } else {
      setMobileOpen(false);
    }
  }, [isDesktop]);

  return (
    <div
      className="flex h-[100dvh] overflow-hidden"
      style={{
        // 桌面端外层 shell 与侧栏共用同一暗色背景，避免连接处出现色差
        background: isDesktop ? "var(--sidebar-shell-bg)" : "var(--bg-canvas)",
      }}
    >
      {/* ── 移动端遮罩 ── */}
      {!isDesktop && (
        <div
          className="fixed inset-0 z-30"
          style={{
            backgroundColor: "rgba(0,0,0,0.5)",
            opacity: mobileOpen ? 1 : 0,
            pointerEvents: mobileOpen ? "auto" : "none",
            transition: "opacity 220ms ease",
          }}
          onClick={() => setMobileOpen(false)}
          aria-hidden
        />
      )}

      {/* ── 侧边栏 ── */}
      {isDesktop ? (
        /* 桌面端：内联折叠，宽度动画 */
        <div
          className="relative shrink-0 overflow-hidden"
          style={{
            width: collapsed ? 0 : 288,
            minWidth: collapsed ? 0 : 288,
            transition: "width 220ms cubic-bezier(0.16,1,0.3,1), min-width 220ms cubic-bezier(0.16,1,0.3,1)",
          }}
        >
          <div style={{ width: 288 }} className="h-full">
            <Suspense fallback={null}>
              <DocsSidebar services={services} onCollapse={handleCollapse} />
            </Suspense>
          </div>
        </div>
      ) : (
        /* 移动端：固定抽屉，滑入动画 */
        <div
          className="fixed inset-y-0 left-0 z-40"
          style={{
            width: 288,
            transform: mobileOpen ? "translateX(0)" : "translateX(-288px)",
            transition: "transform 220ms cubic-bezier(0.16,1,0.3,1)",
          }}
        >
          <Suspense fallback={null}>
            <DocsSidebar services={services} onCollapse={handleCollapse} />
          </Suspense>
        </div>
      )}

      {/* ── 主内容区：桌面端嵌入暗色 chrome 中 ──
       * - 上/右/下保留暗色外框，左侧贴合侧栏，避免连接处出现独立暗槽
       * - 分层阴影保持轻量，主要靠 1px 外环 + 顶部高光建立层级
       */}
      <main
        className="relative flex flex-1 flex-col overflow-hidden"
        style={{
          background: "var(--bg-canvas)",
          marginTop: isDesktop ? 8 : 0,
          marginRight: isDesktop ? 8 : 0,
          marginBottom: isDesktop ? 8 : 0,
          marginLeft: 0,
          borderRadius: isDesktop ? 12 : 0,
          boxShadow: isDesktop
            ? [
                "0 0 0 1px var(--border-default)",
                "0 1px 2px rgba(0, 0, 0, 0.16)",
                "0 14px 34px -26px rgba(0, 0, 0, 0.50)",
                "inset 0 1px 0 0 rgba(255, 255, 255, 0.08)",
              ].join(", ")
            : "none",
        }}
      >
        {/* 移动端顶栏（汉堡菜单 + 品牌） */}
        {!isDesktop && (
          <div
            className="flex shrink-0 items-center gap-3 border-b px-4 py-3"
            style={{
              borderColor: "var(--border-default)",
              background: "var(--bg-canvas)",
            }}
          >
            <button
              onClick={() => setMobileOpen(true)}
              aria-label="打开侧边栏"
              className="flex items-center justify-center rounded-md p-1.5 text-[var(--text-tertiary)] transition-colors hover:bg-[var(--bg-subtle)] hover:text-[var(--text-primary)]"
            >
              <List size={18} />
            </button>
            <div className="flex items-center gap-2">
              <div
                className="flex h-5 w-5 shrink-0 items-center justify-center rounded"
                style={{ background: "var(--accent)" }}
              >
                <svg width="10" height="10" viewBox="0 0 12 12" fill="none" aria-hidden>
                  <path d="M2 3h8M2 6h5M2 9h7" stroke="var(--accent-on)" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
              </div>
              <span className="text-[13px] font-semibold tracking-tight text-[var(--text-secondary)]">APIPrism</span>
            </div>
          </div>
        )}

        {/* 桌面端展开按钮（仅折叠时显示） */}
        {isDesktop && (
          <button
            onClick={() => setCollapsed(false)}
            aria-label="展开侧边栏"
            aria-hidden={!collapsed}
            tabIndex={collapsed ? 0 : -1}
            className="absolute left-3 top-3 z-40 flex h-8 w-8 cursor-pointer items-center justify-center rounded-md text-[var(--text-tertiary)] transition-all duration-150 hover:bg-[var(--bg-subtle)] hover:text-[var(--text-primary)]"
            style={{
              opacity: collapsed ? 1 : 0,
              pointerEvents: collapsed ? "auto" : "none",
              transitionProperty: "opacity, background-color, color",
            }}
          >
            <SidebarSimple size={16} weight="regular" />
          </button>
        )}

        {/* 可滚动内容区 */}
        <div className="flex-1 overflow-y-auto">{children}</div>
      </main>
    </div>
  );
}
