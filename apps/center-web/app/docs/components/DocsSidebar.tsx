"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useCallback, useRef, useEffect } from "react";
import { ServiceCatalogItem, CanonicalGroup } from "../lib/api";
import { MethodBadge } from "./MethodBadge";
import {
  Folder,
  User,
  ShoppingCart,
  CreditCard,
  Gear,
  Shield,
  Bell,
  ChartBar,
  Database,
  FileText,
  Globe,
  Key,
  Lightning,
  ListBullets,
  MagnifyingGlass,
  Tag,
  Upload,
  Plugs,
  ArrowLeft,
  CaretRight,
  SidebarSimple,
} from "@phosphor-icons/react";
import type { Icon } from "@phosphor-icons/react";

// 从 /docs/[service]/[environment]/... 中解析当前项目上下文
function parseDocsRoute(pathname: string): { service: string | null; environment: string | null } {
  const match = pathname.match(/^\/docs\/([^/]+)\/([^/]+)/);
  if (!match) return { service: null, environment: null };
  return {
    service: decodeURIComponent(match[1]),
    environment: decodeURIComponent(match[2]),
  };
}

// 环境色映射
function getEnvTheme(env: string) {
  const lower = env.toLowerCase();
  if (lower === "production" || lower === "prod") {
    return { dot: "#34C759", bg: "rgba(52,199,89,0.12)", border: "rgba(52,199,89,0.22)", text: "#4ADE80" };
  }
  if (lower === "staging" || lower === "preview" || lower === "pre") {
    return { dot: "#FF9F0A", bg: "rgba(255,159,10,0.12)", border: "rgba(255,159,10,0.22)", text: "#FBB042" };
  }
  if (lower === "test" || lower === "testing") {
    return { dot: "#A78BFA", bg: "rgba(167,139,250,0.12)", border: "rgba(167,139,250,0.22)", text: "#A78BFA" };
  }
  return { dot: "#888888", bg: "rgba(136,136,136,0.12)", border: "rgba(136,136,136,0.20)", text: "#AAAAAA" };
}

// ── 分组名 → 图标的智能映射 ──
const GROUP_ICON_PATTERNS: Array<{ keywords: string[]; icon: Icon }> = [
  { keywords: ["user", "account", "profile", "member", "auth"], icon: User },
  { keywords: ["order", "cart", "shop", "purchase", "commerce"], icon: ShoppingCart },
  { keywords: ["pay", "billing", "invoice", "charge", "subscription"], icon: CreditCard },
  { keywords: ["setting", "config", "preference", "option"], icon: Gear },
  { keywords: ["security", "permission", "role", "access", "acl"], icon: Shield },
  { keywords: ["notification", "alert", "message", "email"], icon: Bell },
  { keywords: ["analytics", "metric", "report", "stat", "dashboard"], icon: ChartBar },
  { keywords: ["data", "storage", "database", "model", "entity"], icon: Database },
  { keywords: ["document", "file", "content", "article", "page"], icon: FileText },
  { keywords: ["api", "endpoint", "webhook", "callback", "hook"], icon: Plugs },
  { keywords: ["search", "query", "filter", "find", "lookup"], icon: MagnifyingGlass },
  { keywords: ["upload", "import", "export", "transfer", "sync"], icon: Upload },
  { keywords: ["tag", "label", "category", "classify"], icon: Tag },
  { keywords: ["key", "token", "credential", "secret"], icon: Key },
  { keywords: ["event", "trigger", "action", "workflow", "task"], icon: Lightning },
  { keywords: ["list", "collection", "catalog", "inventory"], icon: ListBullets },
  { keywords: ["region", "locale", "i18n", "country", "geo"], icon: Globe },
];

function getGroupIcon(groupName: string): Icon {
  const lower = groupName.toLowerCase();
  for (const entry of GROUP_ICON_PATTERNS) {
    if (entry.keywords.some((kw) => lower.includes(kw))) {
      return entry.icon;
    }
  }
  return Folder;
}

// ── 分组节点（含懒加载接口列表） ──
function GroupItem({
  service,
  environment,
  group,
  groupDisplayName,
  currentPath,
}: {
  service: string;
  environment: string;
  group: string;
  groupDisplayName: string;
  currentPath: string;
}) {
  const groupHref = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(group)}`;

  // isOpen 由两个信号共同决定：
  // 1. URL 命中分组路径（刷新页面也能保持展开）
  // 2. 本地点击状态（用户点击后立即展开，无需等待导航完成）
  const isUrlActive = currentPath.startsWith(groupHref);
  const [localOpen, setLocalOpen] = useState<boolean | null>(null);
  const isOpen = localOpen ?? isUrlActive;

  const isGroupPageActive = currentPath === groupHref;

  const [operations, setOperations] = useState<CanonicalGroup["operations"] | null>(null);
  const [loading, setLoading] = useState(false);
  const fetchedRef = useRef(false);

  // isOpen 变为 true 时懒加载接口列表（点击或 URL 驱动均触发）
  const fetchOps = useCallback(async () => {
    if (fetchedRef.current) return;
    fetchedRef.current = true;
    setLoading(true);
    try {
      const res = await fetch(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/groups/${encodeURIComponent(group)}`,
        { cache: "no-store" }
      );
      if (res.ok) {
        const data: CanonicalGroup = await res.json();
        setOperations(data.operations ?? []);
      } else {
        setOperations([]);
      }
    } catch {
      setOperations([]);
    } finally {
      setLoading(false);
    }
  }, [service, environment, group]);

  useEffect(() => {
    if (isOpen) fetchOps();
  }, [isOpen, fetchOps]);

  // 检查当前路径是否有激活的子项
  const hasActiveChild = operations?.some((op) => {
    const opHref = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(group)}/${encodeURIComponent(op.operationId)}`;
    return currentPath === opHref;
  }) ?? false;

  const isHighlighted = isGroupPageActive || hasActiveChild;
  const GroupIcon = getGroupIcon(groupDisplayName);

  return (
    <li>
      {/* ── 分组行：Link 跳转落地页 + 同步展开接口列表 ── */}
      <Link
        href={groupHref}
        onClick={() => setLocalOpen((prev) => !(prev ?? isUrlActive))}
        className="group/nav flex cursor-pointer items-center gap-2.5 rounded-lg px-2.5 py-2 transition-colors duration-150 hover:bg-white/[0.05]"
        style={{
          backgroundColor: isHighlighted ? "rgba(255,255,255,0.06)" : undefined,
        }}
      >
        <span
          className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md transition-all duration-200"
          style={{
            backgroundColor: isHighlighted ? "rgba(124,58,237,0.15)" : "rgba(255,255,255,0.05)",
            color: isHighlighted ? "#A78BFA" : "rgba(255,255,255,0.35)",
            boxShadow: "none",
          }}
        >
          <GroupIcon size={14} weight={isHighlighted ? "fill" : "regular"} />
        </span>
        <span
          className="flex-1 truncate text-[13.5px] font-medium leading-snug transition-colors duration-150 group-hover/nav:text-white/80"
          style={{
            color: isHighlighted ? "rgba(255,255,255,0.95)" : "rgba(255,255,255,0.55)",
          }}
        >
          {groupDisplayName}
        </span>
        {operations && operations.length > 0 && (
          <span className="shrink-0 font-mono text-[10px] tabular-nums text-white/20">
            {operations.length}
          </span>
        )}
        <CaretRight
          size={12}
          weight="bold"
          className="shrink-0 text-white/20 transition-transform duration-200 ease-out"
          style={{ transform: isOpen ? "rotate(90deg)" : "rotate(0deg)" }}
        />
      </Link>

      {/* ── 接口列表：CSS grid 高度动画 ── */}
      <div
        className="grid transition-[grid-template-rows] duration-200 ease-out"
        style={{ gridTemplateRows: isOpen ? "1fr" : "0fr" }}
      >
        <div className="overflow-hidden">
          <ul className="mt-0.5 space-y-px pl-[18px]">
            {/* 加载骨架 */}
            {loading &&
              [0, 1, 2].map((i) => (
                <li key={i} className="flex items-center gap-2.5 rounded-md px-2.5 py-[7px]">
                  <span
                    className="inline-block h-[14px] w-8 animate-pulse rounded"
                    style={{
                      backgroundColor: "rgba(255,255,255,0.05)",
                      animationDelay: `${i * 80}ms`,
                    }}
                  />
                  <span
                    className="inline-block h-[13px] flex-1 animate-pulse rounded"
                    style={{
                      backgroundColor: "rgba(255,255,255,0.03)",
                      animationDelay: `${i * 80 + 40}ms`,
                    }}
                  />
                </li>
              ))}

            {/* 接口项 */}
            {!loading &&
              operations?.map((op) => {
                const opHref = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(group)}/${encodeURIComponent(op.operationId)}`;
                const isActive = currentPath === opHref;
                return (
                  <li key={op.operationId}>
                    <Link
                      href={opHref}
                      title={op.summary || op.operationId}
                      className="flex cursor-pointer items-center gap-2.5 rounded-md px-2.5 py-[7px] transition-colors duration-150 hover:bg-white/[0.05]"
                      style={{
                        backgroundColor: isActive ? "rgba(124,58,237,0.10)" : undefined,
                        borderLeft: isActive ? "2px solid #7C3AED" : "2px solid transparent",
                      }}
                    >
                      <MethodBadge method={op.method} size="sm" />
                      <span
                        className="truncate text-[12.5px] leading-snug transition-colors duration-150"
                        style={{
                          color: isActive ? "#C4B5FD" : "rgba(255,255,255,0.45)",
                          fontWeight: isActive ? 500 : 400,
                        }}
                      >
                        {op.summary || op.path}
                      </span>
                    </Link>
                  </li>
                );
              })}

            {/* 空状态 */}
            {!loading && operations?.length === 0 && (
              <li className="px-2.5 py-2 text-[12px] text-white/20">
                暂无接口
              </li>
            )}
          </ul>
        </div>
      </div>
    </li>
  );
}

// ── 侧边栏主体 ──
export function DocsSidebar({
  services,
  onCollapse,
}: {
  services: ServiceCatalogItem[];
  onCollapse?: () => void;
}) {
  const pathname = usePathname();
  const { service: currentServiceName, environment: currentEnvironment } = parseDocsRoute(pathname);

  const activeService =
    currentServiceName && currentEnvironment
      ? services.find(
          (s) => s.name === currentServiceName && s.environment === currentEnvironment
        ) ?? null
      : null;

  const envTheme = activeService ? getEnvTheme(activeService.environment) : null;

  return (
    <aside
      className="flex h-full w-[288px] shrink-0 flex-col"
      style={{
        backgroundColor: "#0A0A0A",
        borderRight: "1px solid #1F1F1F",
      }}
    >
      {/* ── 头部：品牌 + 服务信息 ── */}
      {activeService && envTheme ? (
        <div
          className="px-4 pt-5 pb-4"
          style={{ borderBottom: "1px solid rgba(255,255,255,0.07)" }}
        >
          {/* APIPrism 品牌 + 收起按钮 */}
          <div className="mb-4 flex items-center justify-between">
            <Link href="/" className="group flex items-center gap-2">
              <div
                className="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-md"
                style={{ background: "#7C3AED" }}
              >
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden>
                  <path d="M2 3h8M2 6h5M2 9h7" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
              </div>
              <span className="text-[13px] font-semibold tracking-tight text-white/70 transition-colors duration-150 group-hover:text-white/50">
                APIPrism
              </span>
            </Link>
            {onCollapse && (
              <button
                onClick={onCollapse}
                aria-label="收起侧边栏"
                className="flex cursor-pointer items-center justify-center rounded-md p-1.5 text-white/22 transition-colors duration-150 hover:bg-white/[0.06] hover:text-white/55"
              >
                <SidebarSimple size={16} weight="regular" />
              </button>
            )}
          </div>

          {/* 服务名 */}
          <h2
            className="truncate text-[15px] font-semibold leading-snug tracking-tight text-white/88"
            title={activeService.name}
          >
            {activeService.name}
          </h2>

          {/* 环境 + 版本 */}
          <div className="mt-2.5 flex items-center gap-2">
            <span
              className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-[3px] font-mono text-[10px] font-medium uppercase tracking-wider"
              style={{
                backgroundColor: envTheme.bg,
                border: `1px solid ${envTheme.border}`,
                color: envTheme.text,
              }}
            >
              <span
                className="inline-flex h-[5px] w-[5px] shrink-0 rounded-full"
                style={{ backgroundColor: envTheme.dot }}
              />
              {activeService.environment}
            </span>
            {activeService.version && (
              <span className="font-mono text-[11px] text-white/22">
                v{activeService.version}
              </span>
            )}
          </div>
        </div>
      ) : (
        <div
          className="flex items-center gap-2.5 px-4 py-4"
          style={{ borderBottom: "1px solid rgba(255,255,255,0.07)" }}
        >
          <Link href="/" className="group flex items-center gap-2">
            <div
              className="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-md"
              style={{ background: "#7C3AED" }}
            >
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden>
                <path d="M2 3h8M2 6h5M2 9h7" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
              </svg>
            </div>
            <span className="text-[13px] font-semibold tracking-tight text-white/80 transition-opacity duration-150 group-hover:opacity-60">
              APIPrism
            </span>
          </Link>
          <span className="font-mono text-[10px] font-medium uppercase tracking-[0.12em] text-white/22">
            目录
          </span>
          {onCollapse && (
            <button
              onClick={onCollapse}
              aria-label="收起侧边栏"
              className="ml-auto flex items-center justify-center rounded-md p-1.5 text-white/22 transition-colors duration-150 hover:bg-white/[0.06] hover:text-white/55"
            >
              <SidebarSimple size={16} weight="regular" />
            </button>
          )}
        </div>
      )}

      {/* ── 分组导航区 ── */}
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        {activeService ? (
          <>
            {/* 区域标签 */}
            <div className="mb-2 px-2.5">
              <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-white/25">
                接口列表
              </span>
            </div>

            {activeService.groups.length === 0 ? (
              <div className="flex flex-col items-center justify-center px-4 py-8 text-center">
                <Folder size={28} weight="thin" className="mb-2 text-white/10" />
                <p className="text-[13px] text-white/30">暂无分组</p>
                <p className="mt-0.5 text-[11px] text-white/15">注册服务后接口将自动显示在此处</p>
              </div>
            ) : (
              <ul className="space-y-0.5">
                {activeService.groups.map((group) => (
                  <GroupItem
                    key={group.slug}
                    service={activeService.name}
                    environment={activeService.environment}
                    group={group.slug}
                    groupDisplayName={group.name}
                    currentPath={pathname}
                  />
                ))}
              </ul>
            )}
          </>
        ) : services.length === 0 ? (
          <div className="flex flex-col items-center justify-center px-4 py-8 text-center">
            <Globe size={28} weight="thin" className="mb-2 text-white/10" />
            <p className="text-[13px] text-white/30">暂无已注册服务</p>
            <p className="mt-0.5 text-[11px] text-white/15">连接服务以开始使用</p>
          </div>
        ) : (
          <>
            {/* 区域标签 */}
            <div className="mb-2 px-2.5">
              <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-white/25">
                服务列表
              </span>
            </div>
            <ul className="space-y-0.5">
              {services.map((s) => {
                const svcTheme = getEnvTheme(s.environment);
                return (
                  <li key={`${s.name}::${s.environment}`}>
                    <Link
                      href={`/docs/${encodeURIComponent(s.name)}/${encodeURIComponent(s.environment)}`}
                      className="group/svc flex cursor-pointer items-center gap-2.5 rounded-lg px-2.5 py-2 transition-colors duration-150 hover:bg-white/[0.05]"
                    >
                      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md transition-colors duration-150 group-hover/svc:text-white/50" style={{ backgroundColor: "rgba(255,255,255,0.05)", color: "rgba(255,255,255,0.35)" }}>
                        <Globe size={14} weight="regular" />
                      </span>
                      <span className="flex-1 truncate text-[13.5px] font-medium text-white/62 transition-colors duration-150 group-hover/svc:text-white/88">
                        {s.name}
                      </span>
                      <span
                        className="shrink-0 rounded-full px-2 py-0.5 font-mono text-[10px] font-medium uppercase"
                        style={{
                          backgroundColor: svcTheme.bg,
                          color: svcTheme.text,
                        }}
                      >
                        {s.environment}
                      </span>
                    </Link>
                  </li>
                );
              })}
            </ul>
          </>
        )}
      </nav>

      {/* ── 底部返回首页 ── */}
      <div
        className="px-4 py-3.5"
        style={{ borderTop: "1px solid rgba(255,255,255,0.06)" }}
      >
        <Link
          href="/"
          className="group/back flex cursor-pointer items-center gap-1.5 text-[12.5px] text-white/28 transition-colors duration-150 hover:text-white/58"
        >
          <ArrowLeft size={14} weight="regular" className="shrink-0 transition-transform duration-150 group-hover/back:-translate-x-0.5" />
          返回首页
        </Link>
      </div>
    </aside>
  );
}
