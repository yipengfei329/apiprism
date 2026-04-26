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
  // 惰性初始化，避免 SSR 不匹配
  const [isDesktop, setIsDesktop] = useState<boolean>(() => {
    if (typeof window === "undefined") return true;
    return window.matchMedia(`(min-width: ${MD_BREAKPOINT}px)`).matches;
  });
  const pathname = usePathname();

  // 监听视口断点变化
  useEffect(() => {
    const mq = window.matchMedia(`(min-width: ${MD_BREAKPOINT}px)`);
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
      className="flex h-screen overflow-hidden"
      style={{
        // Arc 风格外层"窗口": 桌面端使用 sidebar 深色作为外框，使主内容区看起来是嵌入式圆角面板
        backgroundColor: isDesktop ? "var(--sidebar-bg)" : "var(--bg-canvas)",
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

      {/* ── 主内容区：桌面端以圆角浮动面板的形式嵌入 Arc 风格暗色 chrome 中 ──
       * - 上/右/下 6px margin 让 sidebar-bg 形成包裹边框
       * - 与侧边栏之间留 6px 凹槽，呼应 Arc 的"窗口分体"观感
       * - 分层阴影：1px 外环 + 顶部高光 + 软投影 强化层次
       */}
      <main
        className="relative flex flex-1 flex-col overflow-hidden"
        style={{
          background: "var(--bg-canvas)",
          marginTop: isDesktop ? 6 : 0,
          marginRight: isDesktop ? 6 : 0,
          marginBottom: isDesktop ? 6 : 0,
          marginLeft: isDesktop ? 6 : 0,
          borderRadius: isDesktop ? 10 : 0,
          boxShadow: isDesktop
            ? [
                "0 0 0 1px var(--border-default)",
                "0 1px 2px rgba(0, 0, 0, 0.4)",
                "0 12px 28px -10px rgba(0, 0, 0, 0.55)",
                "inset 0 1px 0 0 rgba(255, 255, 255, 0.04)",
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
            className="absolute left-3 top-3 z-20 flex cursor-pointer items-center justify-center rounded-md p-1.5 text-[var(--text-tertiary)] transition-all duration-150 hover:bg-[var(--bg-subtle)] hover:text-[var(--text-primary)]"
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
