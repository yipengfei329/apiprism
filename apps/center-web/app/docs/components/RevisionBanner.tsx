"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

interface RevisionBannerProps {
  service: string;
  environment: string;
  revisionId: string;
  seq: number | null;
  registeredAt: string | null;
}

export function RevisionBanner({
  service,
  environment,
  revisionId,
  seq,
  registeredAt,
}: RevisionBannerProps) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const baseHref = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}`;

  const handleActivate = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/rev/${encodeURIComponent(revisionId)}/activate`,
        { method: "POST" }
      );
      if (!res.ok) {
        const body = await res.text().catch(() => "");
        throw new Error(body || `Failed with status ${res.status}`);
      }
      // 回滚成功后清掉 query，让 page 拉 current
      router.push(baseHref);
      router.refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "恢复失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="sticky top-0 z-10 mb-6 rounded-lg border border-[var(--env-staging-border)] bg-[var(--env-staging-bg)] px-4 py-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-[13px] text-[var(--env-staging-text)]">
          <svg className="h-4 w-4 shrink-0" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 1.5a.75.75 0 0 1 .67.41l6 11.5A.75.75 0 0 1 14 14.5H2a.75.75 0 0 1-.67-1.09l6-11.5A.75.75 0 0 1 8 1.5zm0 4a.75.75 0 0 0-.75.75V9a.75.75 0 0 0 1.5 0V6.25A.75.75 0 0 0 8 5.5zm0 6.25a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5z" />
          </svg>
          <span>
            正在查看历史版本
            {seq != null && (
              <span className="ml-1 font-mono font-semibold">#{seq}</span>
            )}
            {registeredAt && (
              <span className="ml-2 text-[var(--env-staging-text)] opacity-80">
                {new Date(registeredAt).toLocaleString()}
              </span>
            )}
            ，页面为只读。
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Link
            href={baseHref}
            className="rounded-md px-2.5 py-1 text-[12px] font-medium text-[var(--env-staging-text)] transition-colors hover:bg-[var(--env-staging-border)]/50"
          >
            返回最新
          </Link>
          <button
            onClick={handleActivate}
            disabled={loading}
            className="rounded-md bg-[var(--env-staging-dot)] px-2.5 py-1 text-[12px] font-medium text-[var(--text-on-accent)] transition-colors hover:opacity-80 disabled:opacity-50"
          >
            {loading ? "恢复中..." : "恢复此版本"}
          </button>
        </div>
      </div>
      {error && (
        <p className="mt-2 text-[12px] text-[var(--danger)]">{error}</p>
      )}
    </div>
  );
}
