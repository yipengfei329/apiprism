"use client";

import { useState, useEffect, useCallback, ReactNode } from "react";
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
  const [isDesktop, setIsDesktop] = useState(true);
  const pathname = usePathname();

  // 监听视口断点变化
  useEffect(() => {
    const mq = window.matchMedia(`(min-width: ${MD_BREAKPOINT}px)`);
    setIsDesktop(mq.matches);
    const handler = (e: MediaQueryListEvent) => {
      setIsDesktop(e.matches);
      if (e.matches) setMobileOpen(false);
    };
    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, []);

  // 移动端路由切换时自动收起侧边栏
  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  const handleCollapse = useCallback(() => {
    if (isDesktop) {
      setCollapsed(true);
    } else {
      setMobileOpen(false);
    }
  }, [isDesktop]);

  // 侧边栏是否可见（用于遮罩控制）
  const sidebarVisible = isDesktop ? !collapsed : mobileOpen;

  return (
    <div className="flex h-screen overflow-hidden">
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
            <DocsSidebar services={services} onCollapse={handleCollapse} />
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
          <DocsSidebar services={services} onCollapse={handleCollapse} />
        </div>
      )}

      {/* ── 主内容区 ── */}
      <main
        className="relative flex flex-1 flex-col overflow-hidden"
        style={{ background: "linear-gradient(180deg, #fafbfc 0%, #ffffff 120px)" }}
      >
        {/* 移动端顶栏（汉堡菜单 + 品牌） */}
        {!isDesktop && (
          <div
            className="flex shrink-0 items-center gap-3 border-b px-4 py-3"
            style={{
              borderColor: "rgba(0,0,0,0.07)",
              background: "rgba(250,251,252,0.95)",
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
            }}
          >
            <button
              onClick={() => setMobileOpen(true)}
              aria-label="打开侧边栏"
              className="flex items-center justify-center rounded-md p-1.5 text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700"
            >
              <List size={18} />
            </button>
            <div className="flex items-center gap-2">
              <div
                className="flex h-5 w-5 shrink-0 items-center justify-center rounded"
                style={{ background: "linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%)" }}
              >
                <svg width="10" height="10" viewBox="0 0 12 12" fill="none" aria-hidden>
                  <path d="M2 3h8M2 6h5M2 9h7" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
              </div>
              <span className="text-[13px] font-semibold tracking-tight text-zinc-700">APIPrism</span>
            </div>
          </div>
        )}

        {/* 桌面端展开按钮（仅折叠时显示） */}
        {isDesktop && (
          <button
            onClick={() => setCollapsed(false)}
            aria-label="展开侧边栏"
            className="absolute left-3 top-3 z-20 flex cursor-pointer items-center justify-center rounded-md p-1.5 text-zinc-400 transition-all duration-150 hover:bg-zinc-100 hover:text-zinc-700"
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
