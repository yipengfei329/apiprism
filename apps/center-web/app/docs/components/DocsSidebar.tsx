"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useState, useCallback, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { ServiceCatalogItem, CanonicalGroup, RevisionSummary } from "../lib/api";
import { MethodBadge } from "./MethodBadge";
import { ThemeToggle } from "../../components/ThemeToggle";
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

// 环境色映射（侧边栏常暗，使用 dark 语义值，不随主题翻转）
// prod = accent teal（承担"最重要信号"角色）
function getEnvTheme(env: string) {
  const lower = env.toLowerCase();
  if (lower === "production" || lower === "prod") {
    return {
      dot: "#2DD4BF",
      bg: "rgba(45,212,191,0.12)",
      border: "rgba(45,212,191,0.28)",
      text: "#2DD4BF",
    };
  }
  if (lower === "staging" || lower === "preview" || lower === "pre") {
    return {
      dot: "#FBBF24",
      bg: "rgba(245,158,11,0.12)",
      border: "rgba(245,158,11,0.22)",
      text: "#FBBF24",
    };
  }
  if (lower === "test" || lower === "testing") {
    return {
      dot: "#A78BFA",
      bg: "rgba(167,139,250,0.14)",
      border: "rgba(167,139,250,0.24)",
      text: "#C4B5FD",
    };
  }
  return {
    dot: "rgba(255,255,255,0.45)",
    bg: "rgba(255,255,255,0.06)",
    border: "rgba(255,255,255,0.10)",
    text: "rgba(255,255,255,0.6)",
  };
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
  revision,
}: {
  service: string;
  environment: string;
  group: string;
  groupDisplayName: string;
  currentPath: string;
  revision: string | null;
}) {
  // 当存在 revision 查询参数时，所有导航都带上该参数，保证在历史版本上下文中浏览
  const querySuffix = revision ? `?revision=${encodeURIComponent(revision)}` : "";
  const groupBasePath = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(group)}`;
  const groupHref = `${groupBasePath}${querySuffix}`;

  // isOpen 由两个信号共同决定：
  // 1. URL 命中分组路径（刷新页面也能保持展开）
  // 2. 本地点击状态（用户点击后立即展开，无需等待导航完成）
  const isUrlActive = currentPath.startsWith(groupBasePath);
  const [localOpen, setLocalOpen] = useState<boolean | null>(null);
  const isOpen = localOpen ?? isUrlActive;

  const isGroupPageActive = currentPath === groupBasePath;

  const [operations, setOperations] = useState<CanonicalGroup["operations"] | null>(null);
  const [loading, setLoading] = useState(false);
  const fetchedRef = useRef(false);

  // revision 切换时重置缓存，强制按新版本重新拉取接口列表
  useEffect(() => {
    fetchedRef.current = false;
    setOperations(null);
  }, [revision]);

  // isOpen 变为 true 时懒加载接口列表（点击或 URL 驱动均触发）
  const fetchOps = useCallback(async () => {
    if (fetchedRef.current) return;
    fetchedRef.current = true;
    setLoading(true);
    try {
      const url = revision
        ? `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/rev/${encodeURIComponent(revision)}/groups/${encodeURIComponent(group)}`
        : `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/groups/${encodeURIComponent(group)}`;
      const res = await fetch(url, { cache: "no-store" });
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
  }, [service, environment, group, revision]);

  useEffect(() => {
    if (isOpen) fetchOps();
  }, [isOpen, fetchOps]);

  // 检查当前路径是否有激活的子项
  const hasActiveChild = operations?.some((op) => {
    const opBasePath = `${groupBasePath}/${encodeURIComponent(op.operationId)}`;
    return currentPath === opBasePath;
  }) ?? false;

  const isHighlighted = isGroupPageActive || hasActiveChild;
  const GroupIcon = getGroupIcon(groupDisplayName);

  return (
    <li>
      {/* ── 分组行：Link 跳转落地页 + 同步展开接口列表 ── */}
      <Link
        href={groupHref}
        onClick={() => setLocalOpen((prev) => !(prev ?? isUrlActive))}
        className="group/nav flex cursor-pointer items-center gap-2.5 rounded-lg px-2.5 py-2 transition-colors duration-150 hover:bg-[var(--sidebar-hover-bg)]"
        style={{
          backgroundColor: isHighlighted ? "var(--sidebar-active-bg)" : undefined,
        }}
      >
        <span
          className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md transition-all duration-200"
          style={{
            backgroundColor: isHighlighted ? "var(--sidebar-accent-bg)" : "var(--sidebar-hover-bg)",
            color: isHighlighted ? "var(--sidebar-accent-text)" : "var(--sidebar-text-tertiary)",
            boxShadow: "none",
          }}
        >
          <GroupIcon size={14} weight={isHighlighted ? "fill" : "regular"} />
        </span>
        <span
          className="flex-1 truncate text-[13.5px] font-medium leading-snug transition-colors duration-150 group-hover/nav:text-[var(--sidebar-text-primary)]"
          style={{
            color: isHighlighted
              ? "var(--sidebar-text-primary)"
              : "var(--sidebar-text-secondary)",
          }}
        >
          {groupDisplayName}
        </span>
        {operations && operations.length > 0 && (
          <span className="shrink-0 font-mono text-[10px] tabular-nums text-[var(--sidebar-text-quaternary)]">
            {operations.length}
          </span>
        )}
        <CaretRight
          size={12}
          weight="bold"
          className="shrink-0 text-[var(--sidebar-text-quaternary)] transition-transform duration-200 ease-out"
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
                      backgroundColor: "var(--sidebar-hover-bg)",
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
                const opBasePath = `${groupBasePath}/${encodeURIComponent(op.operationId)}`;
                const opHref = `${opBasePath}${querySuffix}`;
                const isActive = currentPath === opBasePath;
                return (
                  <li key={op.operationId}>
                    <Link
                      href={opHref}
                      title={op.summary || op.operationId}
                      className="flex cursor-pointer items-center gap-2.5 rounded-md px-2.5 py-[7px] transition-colors duration-150 hover:bg-[var(--sidebar-hover-bg)]"
                      style={{
                        backgroundColor: isActive ? "var(--sidebar-accent-bg)" : undefined,
                        borderLeft: isActive
                          ? "2px solid var(--sidebar-accent-rail)"
                          : "2px solid transparent",
                      }}
                    >
                      <MethodBadge method={op.method} size="sm" />
                      <span
                        className="truncate text-[12.5px] leading-snug transition-colors duration-150"
                        style={{
                          color: isActive
                            ? "var(--sidebar-accent-text)"
                            : "var(--sidebar-text-tertiary)",
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
              <li className="px-2.5 py-2 text-[12px] text-[var(--sidebar-text-quaternary)]">
                暂无接口
              </li>
            )}
          </ul>
        </div>
      </div>
    </li>
  );
}

// ── 相对时间格式化（与 RevisionSwitcher 保持一致）──
function formatRelative(iso: string) {
  const diff = Date.now() - new Date(iso).getTime();
  const sec = Math.floor(diff / 1000);
  if (sec < 60) return `${sec}s ago`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}m ago`;
  const hour = Math.floor(min / 60);
  if (hour < 24) return `${hour}h ago`;
  const day = Math.floor(hour / 24);
  if (day < 30) return `${day}d ago`;
  const month = Math.floor(day / 30);
  if (month < 12) return `${month}mo ago`;
  return new Date(iso).toLocaleDateString();
}

// ── 侧边栏版本切换按钮（portal dropdown，不受父级 overflow:hidden 约束）──
function SidebarRevisionButton({
  service,
  environment,
  viewingRevisionId,
}: {
  service: string;
  environment: string;
  viewingRevisionId: string | null;
}) {
  const [open, setOpen] = useState(false);
  const [revisions, setRevisions] = useState<RevisionSummary[] | null>(null);
  const [dropdownPos, setDropdownPos] = useState<{ top: number; left: number }>({ top: 0, left: 0 });
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetch(
      `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/rev`,
      { cache: "no-store" }
    )
      .then((r) => (r.ok ? r.json() : []))
      .then(setRevisions)
      .catch(() => setRevisions([]));
  }, [service, environment]);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (e: PointerEvent) => {
      const t = e.target as Node;
      if (!buttonRef.current?.contains(t) && !dropdownRef.current?.contains(t)) {
        setOpen(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  if (!revisions || revisions.length === 0) return null;

  const active = revisions.find((r) =>
    viewingRevisionId ? r.id === viewingRevisionId : r.current
  );
  const viewingOlder = Boolean(viewingRevisionId && active && !active.current);
  const baseHref = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}`;

  const handleToggle = () => {
    if (!open && buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      setDropdownPos({ top: rect.bottom + 6, left: rect.left });
    }
    setOpen((v) => !v);
  };

  return (
    <>
      <button
        ref={buttonRef}
        onClick={handleToggle}
        className="group/rev inline-flex cursor-pointer items-center gap-1 rounded-md border px-2.5 py-[3px] font-mono text-[10px] font-medium text-[var(--sidebar-text-secondary)] transition-colors duration-150 hover:border-[var(--sidebar-accent-rail)] hover:text-[var(--sidebar-accent-text)]"
        style={{ borderColor: "rgba(255,255,255,0.10)" }}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        {active ? `#${active.seq}` : "rev"}
        {viewingOlder && (
          <span className="rounded bg-[var(--env-staging-bg)] px-1 text-[9px] text-[var(--env-staging-text)]">
            历史
          </span>
        )}
        <svg className="h-2.5 w-2.5 shrink-0 opacity-50 transition-opacity group-hover/rev:opacity-100" viewBox="0 0 12 12" fill="none" aria-hidden>
          <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {open && createPortal(
        <div
          ref={dropdownRef}
          role="listbox"
          className="fixed z-[9999] max-h-[280px] w-[268px] overflow-y-auto rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface)] py-1 shadow-lg"
          style={dropdownPos}
        >
          {revisions.map((rev) => {
            const href = rev.current
              ? baseHref
              : `${baseHref}?revision=${encodeURIComponent(rev.id)}`;
            return (
              <Link
                key={rev.id}
                href={href}
                role="option"
                onClick={() => setOpen(false)}
                className="flex items-center justify-between gap-2 px-3 py-2 hover:bg-[var(--bg-subtle)]"
              >
                <div className="flex items-center gap-2">
                  <span className="font-mono text-[12px] font-semibold text-[var(--text-primary)]">
                    #{rev.seq}
                  </span>
                  {rev.current && (
                    <span className="rounded bg-[var(--env-prod-bg)] px-1.5 py-[1px] font-mono text-[10px] font-medium text-[var(--env-prod-text)]">
                      当前
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2 text-[11px] text-[var(--text-tertiary)]">
                  <span className="font-mono">{rev.specHash.substring(0, 7)}</span>
                  <span>·</span>
                  <span>{formatRelative(rev.registeredAt)}</span>
                </div>
              </Link>
            );
          })}
        </div>,
        document.body
      )}
    </>
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
  const searchParams = useSearchParams();
  const revision = searchParams.get("revision");
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
        backgroundColor: "var(--sidebar-bg)",
      }}
    >
      {/* ── 头部：品牌 + 服务信息 ── */}
      {activeService && envTheme ? (
        <div
          className="px-4 pt-4 pb-4"
          style={{ borderBottom: "1px solid var(--sidebar-border)" }}
        >
          {/* APIPrism 品牌 + 主题切换 + 收起按钮 */}
          <div className="mb-3 flex items-center justify-between">
            <Link href="/" className="group flex items-center gap-2">
              <div
                className="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-md"
                style={{ background: "var(--sidebar-accent-rail)" }}
              >
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden>
                  <path d="M2 3h8M2 6h5M2 9h7" stroke="#042F2E" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
              </div>
              <span className="text-[13px] font-semibold tracking-tight text-[var(--sidebar-text-secondary)] transition-colors duration-150 group-hover:text-[var(--sidebar-text-primary)]">
                APIPrism
              </span>
            </Link>
            {onCollapse && (
              <button
                onClick={onCollapse}
                aria-label="收起侧边栏"
                className="flex h-7 w-7 cursor-pointer items-center justify-center rounded-md text-[var(--sidebar-text-quaternary)] transition-colors duration-150 hover:bg-[var(--sidebar-hover-bg)] hover:text-[var(--sidebar-text-secondary)]"
              >
                <SidebarSimple size={16} weight="regular" />
              </button>
            )}
          </div>

          {/* 服务名 */}
          <h2
            className="truncate text-[15px] font-semibold leading-snug tracking-tight text-[var(--sidebar-text-primary)]"
            title={activeService.name}
          >
            {activeService.name}
          </h2>

          {/* 环境 + 版本 */}
          <div className="mt-2.5 flex items-center gap-2">
            <span
              className="inline-flex items-center gap-1.5 rounded-md px-2.5 py-[3px] font-mono text-[10px] font-medium uppercase tracking-wider"
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
            <SidebarRevisionButton
              service={activeService.name}
              environment={activeService.environment}
              viewingRevisionId={revision}
            />
          </div>
        </div>
      ) : (
        <div
          className="flex items-center gap-2.5 px-4 py-4"
          style={{ borderBottom: "1px solid var(--sidebar-border)" }}
        >
          <Link href="/" className="group flex items-center gap-2">
            <div
              className="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-md"
              style={{ background: "var(--sidebar-accent-rail)" }}
            >
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden>
                <path d="M2 3h8M2 6h5M2 9h7" stroke="#042F2E" strokeWidth="1.5" strokeLinecap="round" />
              </svg>
            </div>
            <span className="text-[13px] font-semibold tracking-tight text-[var(--sidebar-text-primary)] transition-opacity duration-150 group-hover:opacity-60">
              APIPrism
            </span>
          </Link>
          <span className="font-mono text-[10px] font-medium uppercase tracking-[0.12em] text-[var(--sidebar-text-quaternary)]">
            目录
          </span>
          {onCollapse && (
            <button
              onClick={onCollapse}
              aria-label="收起侧边栏"
              className="ml-auto flex h-7 w-7 items-center justify-center rounded-md text-[var(--sidebar-text-quaternary)] transition-colors duration-150 hover:bg-[var(--sidebar-hover-bg)] hover:text-[var(--sidebar-text-secondary)]"
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
              <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--sidebar-text-quaternary)]">
                接口列表
              </span>
            </div>

            {activeService.groups.length === 0 ? (
              <div className="flex flex-col items-center justify-center px-4 py-8 text-center">
                <Folder size={28} weight="thin" className="mb-2 text-[var(--sidebar-text-quaternary)]" />
                <p className="text-[13px] text-[var(--sidebar-text-tertiary)]">暂无分组</p>
                <p className="mt-0.5 text-[11px] text-[var(--sidebar-text-quaternary)]">注册服务后接口将自动显示在此处</p>
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
                    revision={revision}
                  />
                ))}
              </ul>
            )}
          </>
        ) : services.length === 0 ? (
          <div className="flex flex-col items-center justify-center px-4 py-8 text-center">
            <Globe size={28} weight="thin" className="mb-2 text-[var(--sidebar-text-quaternary)]" />
            <p className="text-[13px] text-[var(--sidebar-text-tertiary)]">暂无已注册服务</p>
            <p className="mt-0.5 text-[11px] text-[var(--sidebar-text-quaternary)]">连接服务以开始使用</p>
          </div>
        ) : (
          <>
            {/* 区域标签 */}
            <div className="mb-2 px-2.5">
              <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--sidebar-text-quaternary)]">
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
                      className="group/svc flex cursor-pointer items-center gap-2.5 rounded-lg px-2.5 py-2 transition-colors duration-150 hover:bg-[var(--sidebar-hover-bg)]"
                    >
                      <span
                        className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md transition-colors duration-150"
                        style={{
                          backgroundColor: "var(--sidebar-hover-bg)",
                          color: "var(--sidebar-text-tertiary)",
                        }}
                      >
                        <Globe size={14} weight="regular" />
                      </span>
                      <span className="flex-1 truncate text-[13.5px] font-medium text-[var(--sidebar-text-secondary)] transition-colors duration-150 group-hover/svc:text-[var(--sidebar-text-primary)]">
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

      {/* ── 底部：返回首页（左） + 主题切换（右下角） ── */}
      <div
        className="flex items-center justify-between px-4 py-3"
        style={{ borderTop: "1px solid var(--sidebar-divider)" }}
      >
        <Link
          href="/"
          className="group/back flex cursor-pointer items-center gap-1.5 text-[12.5px] text-[var(--sidebar-text-tertiary)] transition-colors duration-150 hover:text-[var(--sidebar-text-primary)]"
        >
          <ArrowLeft size={14} weight="regular" className="shrink-0 transition-transform duration-150 group-hover/back:-translate-x-0.5" />
          返回首页
        </Link>
        <ThemeToggle variant="sidebar" />
      </div>
    </aside>
  );
}
