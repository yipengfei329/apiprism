"use client";

import { useState, ReactNode } from "react";
import { ServiceCatalogItem } from "../lib/api";
import { DocsSidebar } from "./DocsSidebar";
import { SidebarSimple } from "@phosphor-icons/react";

export function SidebarLayout({
  services,
  children,
}: {
  services: ServiceCatalogItem[];
  children: ReactNode;
}) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden">
      {/* ── 侧边栏包装层，宽度通过 transition 动画 ── */}
      <div
        className="relative shrink-0 overflow-hidden"
        style={{
          width: collapsed ? 0 : 288,
          minWidth: collapsed ? 0 : 288,
          transition: "width 220ms cubic-bezier(0.16,1,0.3,1), min-width 220ms cubic-bezier(0.16,1,0.3,1)",
        }}
      >
        {/* 侧边栏内容固定宽度，不随动画缩放 */}
        <div style={{ width: 288 }} className="h-full">
          <DocsSidebar services={services} onCollapse={() => setCollapsed(true)} />
        </div>
      </div>

      {/* ── 主内容区 ── */}
      <main className="relative flex-1 overflow-hidden" style={{ background: "linear-gradient(180deg, #fafbfc 0%, #ffffff 120px)" }}>
        {/* 展开按钮（仅折叠时显示），置于滚动容器之外以避免被滚动或遮挡 */}
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

        <div className="h-full overflow-y-auto">{children}</div>
      </main>
    </div>
  );
}
