"use client";

import { useCallback, useState } from "react";
import { Copy, Check, Lightning } from "@phosphor-icons/react";
import { getPublicApiUrl } from "@/app/lib/public-api";

interface McpToggleProps {
  service: string;
  environment: string;
  /** 分组 slug，undefined 表示服务级 */
  groupSlug?: string;
  initialEnabled: boolean;
  initialSseEndpoint: string | null;
  initialStreamableEndpoint: string | null;
}

export function McpToggle({
  service,
  environment,
  groupSlug,
  initialEnabled,
  initialSseEndpoint,
  initialStreamableEndpoint,
}: McpToggleProps) {
  const [enabled, setEnabled] = useState(initialEnabled);
  const [sseEndpoint, setSseEndpoint] = useState(initialSseEndpoint);
  const [streamableEndpoint, setStreamableEndpoint] = useState(initialStreamableEndpoint);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const toggleUrl = groupSlug
    ? getPublicApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(groupSlug)}/mcp-config`
      )
    : getPublicApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/mcp-config`
      );

  const handleToggle = useCallback(async () => {
    const next = !enabled;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(toggleUrl, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ enabled: next }),
      });
      if (res.ok) {
        const data = await res.json();
        setEnabled(data.enabled);
        setSseEndpoint(data.sseEndpoint ?? null);
        setStreamableEndpoint(data.streamableEndpoint ?? null);
      } else {
        const body = await res.json().catch(() => null);
        setError(body?.detail ?? body?.message ?? `启用失败 (HTTP ${res.status})`);
      }
    } catch {
      setError("网络请求失败");
    } finally {
      setLoading(false);
    }
  }, [enabled, toggleUrl]);

  const copyText = useCallback((text: string, key: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(key);
      setTimeout(() => setCopied(null), 1500);
    });
  }, []);

  return (
    <div className="rounded-xl bg-white/90 backdrop-blur-sm v-card-full p-5">
      {/* 标题行 + 开关 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Lightning size={16} weight="fill" className={enabled ? "text-amber-500" : "text-v-gray-400"} />
          <span className="text-[13px] font-semibold text-v-black">MCP 服务</span>
          <span className="text-[11px] text-v-gray-400">
            {groupSlug ? "分组级" : "服务级"}
          </span>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={enabled}
          disabled={loading}
          onClick={handleToggle}
          className={`relative inline-flex h-[24px] w-[44px] shrink-0 cursor-pointer rounded-full
                      transition-colors duration-200 ease-in-out focus:outline-none
                      ${enabled ? "bg-[#34C759]" : "bg-[#E5E5EA]"}
                      ${loading ? "opacity-60 cursor-wait" : ""}`}
        >
          <span
            className={`pointer-events-none inline-block h-[20px] w-[20px] rounded-full bg-white
                        shadow-sm ring-0 transition-transform duration-200 ease-in-out
                        ${enabled ? "translate-x-[22px]" : "translate-x-[2px]"} mt-[2px]`}
          />
        </button>
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-[12px] text-red-700">
          {error}
        </div>
      )}

      {/* 端点地址 */}
      {enabled && (sseEndpoint || streamableEndpoint) && (
        <div className="mt-4 space-y-2">
          {sseEndpoint && (
            <EndpointRow
              label="SSE"
              url={sseEndpoint}
              copied={copied === "sse"}
              onCopy={() => copyText(sseEndpoint, "sse")}
            />
          )}
          {streamableEndpoint && (
            <EndpointRow
              label="Streamable HTTP"
              url={streamableEndpoint}
              copied={copied === "stream"}
              onCopy={() => copyText(streamableEndpoint, "stream")}
            />
          )}
        </div>
      )}
    </div>
  );
}

function EndpointRow({
  label,
  url,
  copied,
  onCopy,
}: {
  label: string;
  url: string;
  copied: boolean;
  onCopy: () => void;
}) {
  return (
    <div className="flex items-center gap-2 rounded-lg bg-[#F4F4F5] px-3 py-2">
      <span className="shrink-0 rounded bg-[#E5E5EA] px-1.5 py-0.5 font-mono text-[10px] font-semibold text-v-gray-500 uppercase">
        {label}
      </span>
      <code className="min-w-0 flex-1 truncate font-mono text-[12px] text-v-black">{url}</code>
      <button
        type="button"
        onClick={onCopy}
        className="flex shrink-0 items-center gap-1 rounded-md px-2 py-1 text-[11px] font-medium
                   text-v-gray-400 transition-colors hover:text-v-gray-600 hover:bg-white/60 cursor-pointer"
      >
        {copied ? (
          <>
            <Check size={13} weight="bold" className="text-green-600" />
            <span className="text-green-600">已复制</span>
          </>
        ) : (
          <>
            <Copy size={13} />
            <span>复制</span>
          </>
        )}
      </button>
    </div>
  );
}
