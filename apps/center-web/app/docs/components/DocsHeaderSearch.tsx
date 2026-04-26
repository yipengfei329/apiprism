"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
} from "react";
import { useRouter } from "next/navigation";
import { createPortal } from "react-dom";
import {
  ArrowBendDownLeft,
  CircleNotch,
  Command,
  Database,
  FileMagnifyingGlass,
  MagnifyingGlass,
  Rows,
  X,
} from "@phosphor-icons/react";
import { CanonicalServiceSnapshot, CanonicalOperation } from "../lib/api";
import { MethodBadge } from "./MethodBadge";

const METHOD_ORDER = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];

// 文档详情页 header 上的搜索入口：按钮 + ⌘K Spotlight 风格弹窗
// 接收当前 service/environment/revision 上下文，搜索范围限定在该服务该版本
export function DocsHeaderSearch({
  service,
  environment,
  revision,
}: {
  service: string;
  environment: string;
  revision: string | null;
}) {
  const [open, setOpen] = useState(false);
  const [snapshot, setSnapshot] = useState<CanonicalServiceSnapshot | null>(null);
  const [snapshotLoading, setSnapshotLoading] = useState(false);

  // 切换服务/环境/版本时丢弃缓存并关闭面板，避免跨上下文串数据
  useEffect(() => {
    setSnapshot(null);
    setOpen(false);
  }, [service, environment, revision]);

  // 懒加载完整 snapshot 用作搜索源；失败时记日志便于线上诊断
  const ensureSnapshot = useCallback(async () => {
    if (snapshot || snapshotLoading) return;
    setSnapshotLoading(true);
    try {
      const base = `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}`;
      const url = revision ? `${base}/rev/${encodeURIComponent(revision)}` : base;
      const res = await fetch(url, { cache: "no-store" });
      if (res.ok) {
        const data = (await res.json()) as CanonicalServiceSnapshot;
        setSnapshot(data);
      } else {
        console.warn("[search] failed to load snapshot", res.status);
      }
    } catch (err) {
      console.warn("[search] snapshot fetch error", err);
    } finally {
      setSnapshotLoading(false);
    }
  }, [service, environment, revision, snapshot, snapshotLoading]);

  // ⌘K / Ctrl+K 全局切换命令面板
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setOpen((v) => !v);
      }
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, []);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="搜索接口 (⌘K)"
        title="搜索接口 (⌘K)"
        className="docs-command-trigger inline-flex h-8 shrink-0 cursor-pointer items-center gap-2 rounded-xl px-2.5 text-[12px] font-medium text-v-gray-500 transition hover:text-v-gray-600 active:translate-y-px"
      >
        <MagnifyingGlass size={13} weight="bold" aria-hidden />
        <span className="hidden sm:inline">搜索</span>
        <kbd
          aria-hidden
          className="hidden h-5 items-center rounded-md border border-v-gray-100 bg-bg-surface px-1.5 font-mono text-[9.5px] text-v-gray-400 shadow-[inset_0_1px_0_rgba(255,255,255,0.72)] sm:inline-flex"
        >
          ⌘K
        </kbd>
      </button>

      {open && (
        <CommandPalette
          onClose={() => setOpen(false)}
          service={service}
          environment={environment}
          revision={revision}
          snapshot={snapshot}
          snapshotLoading={snapshotLoading}
          ensureSnapshot={ensureSnapshot}
        />
      )}
    </>
  );
}

// ── 命令面板：⌘K 触发的 Spotlight 风格搜索弹窗 ──
// 由父级条件渲染（mount = open），unmount 自动清理状态/副作用
// snapshot 缓存留在父组件，避免反复开关重复拉取
function CommandPalette({
  onClose,
  service,
  environment,
  revision,
  snapshot,
  snapshotLoading,
  ensureSnapshot,
}: {
  onClose: () => void;
  service: string;
  environment: string;
  revision: string | null;
  snapshot: CanonicalServiceSnapshot | null;
  snapshotLoading: boolean;
  ensureSnapshot: () => void | Promise<void>;
}) {
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const totalOperations = useMemo(
    () => snapshot?.groups.reduce((acc, g) => acc + g.operations.length, 0) ?? 0,
    [snapshot]
  );

  const methodCounts = useMemo(() => {
    if (!snapshot) return [];
    const counts = new Map<string, number>();
    for (const group of snapshot.groups) {
      for (const op of group.operations) {
        const method = op.method.toUpperCase();
        counts.set(method, (counts.get(method) ?? 0) + 1);
      }
    }
    return [...counts.entries()]
      .sort(([a], [b]) => {
        const ai = METHOD_ORDER.indexOf(a);
        const bi = METHOD_ORDER.indexOf(b);
        return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi);
      })
      .slice(0, 6);
  }, [snapshot]);

  // mount 时拉取 snapshot 并把焦点交给输入框
  useEffect(() => {
    ensureSnapshot();
    requestAnimationFrame(() => {
      inputRef.current?.focus();
      inputRef.current?.select();
    });
  }, [ensureSnapshot]);

  // 锁定 body 滚动 + 监听 Esc 关闭（mount/unmount 自动管理）
  useEffect(() => {
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = prevOverflow;
      document.removeEventListener("keydown", onKey);
    };
  }, [onClose]);

  // 模糊匹配 method/path/summary/operationId/分组名；上限 80 条
  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q || !snapshot) return [];
    const out: Array<{ groupName: string; groupSlug: string; op: CanonicalOperation }> = [];
    for (const g of snapshot.groups) {
      for (const op of g.operations) {
        const haystack = `${op.method} ${op.path} ${op.summary} ${op.operationId} ${g.name}`.toLowerCase();
        if (haystack.includes(q)) {
          out.push({ groupName: g.name, groupSlug: g.slug, op });
        }
      }
    }
    return out.slice(0, 80);
  }, [query, snapshot]);

  const isSearching = query.trim().length > 0;
  const safeActiveIndex = results.length === 0 ? 0 : Math.min(activeIndex, results.length - 1);

  // 选中项滚动到可见区域
  useEffect(() => {
    const item = listRef.current?.children[safeActiveIndex] as HTMLElement | undefined;
    item?.scrollIntoView({ block: "nearest" });
  }, [safeActiveIndex]);

  const navigate = useCallback(
    (r: { groupSlug: string; op: CanonicalOperation }) => {
      const querySuffix = revision ? `?revision=${encodeURIComponent(revision)}` : "";
      const href = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(r.groupSlug)}/${encodeURIComponent(r.op.operationId)}${querySuffix}`;
      onClose();
      router.push(href);
    },
    [service, environment, revision, router, onClose]
  );

  const onInputKeyDown = (e: ReactKeyboardEvent<HTMLInputElement>) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((i) => (results.length === 0 ? 0 : Math.min(i + 1, results.length - 1)));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((i) => Math.max(i - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const r = results[safeActiveIndex];
      if (r) navigate(r);
    }
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[10000] flex items-start justify-center px-3 pt-[9dvh] sm:px-4 sm:pt-[12dvh]"
      role="dialog"
      aria-modal="true"
      aria-label="搜索接口"
    >
      {/* 遮罩：点击关闭 */}
      <div className="docs-command-overlay absolute inset-0 cursor-pointer" onClick={onClose} aria-hidden />

      <div className="docs-command-panel relative w-full max-w-[720px] overflow-hidden rounded-[18px]">
        <div className="flex items-center justify-between gap-4 border-b border-white/[0.07] px-4 py-3.5 sm:px-5">
          <div className="flex min-w-0 items-center gap-3">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl border border-white/[0.08] bg-white/[0.045] text-[var(--sidebar-accent-text)] shadow-[inset_0_1px_0_rgba(255,255,255,0.07)]">
              <Command size={15} weight="bold" aria-hidden />
            </span>
            <div className="min-w-0">
              <div className="text-[13px] font-semibold text-[var(--sidebar-text-primary)]">
                搜索当前 API 文档
              </div>
              <div className="mt-0.5 flex min-w-0 items-center gap-1.5 font-mono text-[10.5px] text-[var(--sidebar-text-quaternary)]">
                <span className="truncate">{service}</span>
                <span aria-hidden>/</span>
                <span className="truncate">{environment}</span>
                {revision && (
                  <>
                    <span aria-hidden>/</span>
                    <span className="truncate">rev {revision}</span>
                  </>
                )}
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="关闭搜索"
            className="flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-xl border border-white/[0.07] bg-white/[0.035] text-[var(--sidebar-text-tertiary)] transition hover:bg-white/[0.065] hover:text-[var(--sidebar-text-primary)] active:translate-y-px"
          >
            <X size={13} weight="bold" />
          </button>
        </div>

        {/* 输入区 */}
        <div className="px-3 py-3 sm:px-4">
          <div className="docs-command-input flex items-center gap-3 rounded-2xl px-3.5 py-3">
            <MagnifyingGlass
              size={18}
              weight="bold"
              aria-hidden
              className="shrink-0 text-[var(--sidebar-text-tertiary)]"
            />
            <input
              ref={inputRef}
              type="text"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setActiveIndex(0);
              }}
              onKeyDown={onInputKeyDown}
              placeholder="搜索 path、摘要、operationId 或分组名"
              spellCheck={false}
              autoComplete="off"
              aria-label="搜索接口"
              className="min-w-0 flex-1 bg-transparent text-[15px] font-medium text-[var(--sidebar-text-primary)] placeholder:text-[var(--sidebar-text-quaternary)] focus:outline-none"
            />
            {snapshotLoading && (
              <CircleNotch
                size={15}
                weight="bold"
                aria-label="加载中"
                className="shrink-0 animate-spin text-[var(--sidebar-accent-text)]"
              />
            )}
            {query && (
              <button
                type="button"
                onClick={() => {
                  setQuery("");
                  inputRef.current?.focus();
                }}
                aria-label="清空查询"
                className="flex h-7 w-7 shrink-0 cursor-pointer items-center justify-center rounded-lg text-[var(--sidebar-text-quaternary)] transition hover:bg-white/[0.06] hover:text-[var(--sidebar-text-secondary)] active:translate-y-px"
              >
                <X size={12} weight="bold" />
              </button>
            )}
          </div>
        </div>

        <div className="border-t border-white/[0.055]">
          {/* 结果区 / 空态 */}
          {isSearching ? (
            <div>
              <div className="flex items-center justify-between gap-3 px-4 pb-2 pt-3 text-[11px] text-[var(--sidebar-text-quaternary)] sm:px-5">
                <span className="inline-flex items-center gap-2">
                  <Rows size={13} weight="bold" aria-hidden />
                  {snapshot && !snapshotLoading
                    ? `${results.length} 条匹配${results.length === 80 ? " · 已截断" : ""}`
                    : "正在建立索引"}
                </span>
                <span className="hidden max-w-[260px] truncate font-mono sm:inline">{query.trim()}</span>
              </div>
              <ul ref={listRef} className="max-h-[50dvh] overflow-y-auto px-2 pb-2 sm:px-3" role="listbox">
                {snapshotLoading && !snapshot && <SearchSkeleton />}
                {!snapshotLoading && snapshot && results.length === 0 && (
                  <li className="px-2 py-10">
                    <EmptySearch query={query} />
                  </li>
                )}
                {results.map((r, i) => {
                  const isActive = i === safeActiveIndex;
                  return (
                    <li key={`${r.groupSlug}::${r.op.operationId}`} role="option" aria-selected={isActive}>
                      <button
                        type="button"
                        onClick={() => navigate(r)}
                        onMouseEnter={() => setActiveIndex(i)}
                        className="docs-command-result group grid w-full cursor-pointer grid-cols-[auto_1fr_auto] items-center gap-3 rounded-[14px] px-3 py-2.5 text-left"
                        data-active={isActive ? "true" : "false"}
                      >
                        <MethodBadge method={r.op.method} size="sm" />
                        <span className="min-w-0">
                          <span className="flex min-w-0 items-center gap-2">
                            <span className="truncate text-[13.5px] font-medium leading-snug text-[var(--sidebar-text-primary)]">
                              {r.op.summary || r.op.path}
                            </span>
                            <span className="hidden shrink-0 rounded-full border border-white/[0.07] bg-white/[0.035] px-2 py-0.5 font-mono text-[10px] text-[var(--sidebar-text-quaternary)] sm:inline">
                              {r.groupName}
                            </span>
                          </span>
                          <span className="mt-1 flex min-w-0 items-center gap-1.5 font-mono text-[11px] text-[var(--sidebar-text-quaternary)]">
                            <span className="truncate text-[var(--sidebar-text-tertiary)]">{r.op.path}</span>
                            {r.op.operationId && (
                              <>
                                <span aria-hidden>·</span>
                                <span className="hidden max-w-[180px] truncate sm:inline">{r.op.operationId}</span>
                              </>
                            )}
                          </span>
                        </span>
                        <span
                          aria-hidden
                          className="hidden h-7 w-7 shrink-0 items-center justify-center rounded-lg border border-white/[0.08] bg-white/[0.045] text-[var(--sidebar-text-tertiary)] transition group-data-[active=true]:text-[var(--sidebar-accent-text)] sm:inline-flex"
                        >
                          <ArrowBendDownLeft size={13} weight="bold" />
                        </span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            </div>
          ) : (
            <div className="px-4 py-8 sm:px-5">
              {snapshotLoading && !snapshot ? (
                <SearchSkeleton />
              ) : (
                <div className="grid gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
                  <div>
                    <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-2xl border border-white/[0.08] bg-white/[0.045] text-[var(--sidebar-accent-text)] shadow-[inset_0_1px_0_rgba(255,255,255,0.08)]">
                      <FileMagnifyingGlass size={20} weight="bold" aria-hidden />
                    </div>
                    <p className="text-[14px] font-semibold text-[var(--sidebar-text-primary)]">
                      输入关键词定位接口
                    </p>
                    <p className="mt-1 max-w-[54ch] text-[12px] leading-5 text-[var(--sidebar-text-tertiary)]">
                      支持 method、path、摘要、operationId 和分组名，结果仅来自当前服务上下文。
                    </p>
                  </div>
                  <div className="rounded-2xl border border-white/[0.07] bg-white/[0.035] px-4 py-3 shadow-[inset_0_1px_0_rgba(255,255,255,0.06)]">
                    <div className="flex items-center gap-2 text-[11px] text-[var(--sidebar-text-quaternary)]">
                      <Database size={13} weight="bold" aria-hidden />
                      可搜索接口
                    </div>
                    <div className="mt-1 font-mono text-2xl font-semibold leading-none text-[var(--sidebar-text-primary)]">
                      {totalOperations}
                    </div>
                  </div>
                  {methodCounts.length > 0 && (
                    <div className="flex flex-wrap gap-2 sm:col-span-2">
                      {methodCounts.map(([method, count]) => (
                        <span
                          key={method}
                          className="inline-flex items-center gap-1.5 rounded-full border border-white/[0.07] bg-white/[0.035] px-2.5 py-1 font-mono text-[10.5px] text-[var(--sidebar-text-tertiary)]"
                        >
                          <span className="font-semibold text-[var(--sidebar-text-secondary)]">{method}</span>
                          {count}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* 底栏：状态 + 键盘提示 */}
        <div className="flex items-center justify-between gap-3 border-t border-white/[0.06] px-4 py-2.5 text-[11px] text-[var(--sidebar-text-quaternary)] sm:px-5">
          <span className="truncate">
            {snapshotLoading
              ? "同步接口索引中"
              : snapshot
              ? `${totalOperations} 个接口 · ${snapshot.groups.length} 个分组`
              : "等待索引"}
          </span>
          <div className="flex shrink-0 items-center gap-2.5 font-mono">
            <ShortcutKey label="↑↓" text="导航" />
            <ShortcutKey label="↵" text="打开" className="hidden sm:inline-flex" />
            <ShortcutKey label="esc" text="关闭" />
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
}

function ShortcutKey({
  label,
  text,
  className = "inline-flex",
}: {
  label: string;
  text: string;
  className?: string;
}) {
  return (
    <span className={`${className} items-center gap-1`}>
      <kbd className="rounded-md border border-white/[0.10] bg-white/[0.045] px-1.5 py-0.5 text-[10px] text-[var(--sidebar-text-tertiary)] shadow-[inset_0_1px_0_rgba(255,255,255,0.06)]">
        {label}
      </kbd>
      {text}
    </span>
  );
}

function SearchSkeleton() {
  return (
    <>
      {Array.from({ length: 5 }).map((_, i) => (
        <li key={i} className="px-2 py-1.5" aria-hidden>
          <div className="grid grid-cols-[54px_1fr] items-center gap-3 rounded-[14px] border border-white/[0.045] bg-white/[0.025] px-3 py-3">
            <div className="docs-command-skeleton h-6 rounded-full" />
            <div className="space-y-2">
              <div className="docs-command-skeleton h-3.5 w-2/3 rounded-full" />
              <div className="docs-command-skeleton h-2.5 w-full rounded-full" />
            </div>
          </div>
        </li>
      ))}
    </>
  );
}

function EmptySearch({ query }: { query: string }) {
  return (
    <div className="mx-auto max-w-sm text-center">
      <div className="mx-auto mb-3 flex h-11 w-11 items-center justify-center rounded-2xl border border-white/[0.08] bg-white/[0.04] text-[var(--sidebar-text-tertiary)]">
        <FileMagnifyingGlass size={19} weight="bold" aria-hidden />
      </div>
      <p className="text-[13.5px] font-semibold text-[var(--sidebar-text-primary)]">
        没有匹配 &ldquo;{query}&rdquo; 的接口
      </p>
      <p className="mt-1 text-[12px] leading-5 text-[var(--sidebar-text-quaternary)]">
        试试更短的路径片段、HTTP 方法或 operationId。
      </p>
    </div>
  );
}
