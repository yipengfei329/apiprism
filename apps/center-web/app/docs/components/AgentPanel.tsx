"use client";

import { useEffect, useReducer } from "react";
import { Robot, CircleNotch, ArrowSquareOut } from "@phosphor-icons/react";
import { CopyButton } from "@/app/components/CopyButton";

interface AgentPanelProps {
  service: string;
  environment: string;
  operationId: string;
}

type State =
  | { status: "loading" }
  | { status: "error" }
  | { status: "done"; markdown: string };

function reducer(_: State, action: State): State {
  return action;
}

export function AgentPanel({ service, environment, operationId }: AgentPanelProps) {
  const [state, dispatch] = useReducer(reducer, { status: "loading" });

  useEffect(() => {
    let cancelled = false;
    const url = `/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(operationId)}/apidocs.md`;
    fetch(url)
      .then((res) => (res.ok ? res.text() : null))
      .then((md) => {
        if (cancelled) return;
        dispatch(md ? { status: "done", markdown: md } : { status: "error" });
      })
      .catch(() => {
        if (!cancelled) dispatch({ status: "error" });
      });
    return () => { cancelled = true; };
  }, [service, environment, operationId]);

  if (state.status === "loading") {
    return (
      <div className="flex flex-col items-center justify-center px-8 py-36 text-center">
        <CircleNotch size={28} className="animate-spin text-v-gray-400" />
        <p className="mt-4 text-[14px] text-v-gray-400">加载中…</p>
      </div>
    );
  }

  if (state.status === "error") {
    return (
      <div className="flex flex-col items-center justify-center px-8 py-36 text-center">
        <div className="mb-5 rounded-2xl bg-v-gray-50 p-5">
          <Robot size={32} className="text-v-gray-400" />
        </div>
        <h2 className="text-[17px] font-semibold text-v-black">加载失败</h2>
        <p className="mt-2.5 max-w-[36ch] text-[14px] leading-relaxed text-v-gray-400">
          无法获取 Agent 文档，请稍后重试。
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* 顶部操作栏 */}
      <div className="mb-4 flex items-center justify-between">
        <p className="text-[13px] text-v-gray-400">
          面向 AI Agent 的接口描述，可直接复制粘贴给 Agent 使用
        </p>
        <div className="flex items-center gap-2">
          <a
            href={`/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(operationId)}/apidocs.md`}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1 rounded-md px-2 py-1 text-[12px] font-medium text-v-gray-400 transition-colors hover:text-v-link"
          >
            <ArrowSquareOut size={13} />
            打开原文
          </a>
          <CopyButton text={state.markdown} />
        </div>
      </div>

      {/* Markdown 原文展示 */}
      <div className="overflow-hidden rounded-xl border border-white/[0.06] bg-[#282c34]">
        <div className="overflow-auto p-5">
          <pre className="whitespace-pre-wrap break-words font-mono text-[13px] leading-[1.75] text-[#abb2bf]">
            {state.markdown}
          </pre>
        </div>
      </div>
    </div>
  );
}
