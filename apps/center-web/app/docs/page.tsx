import { redirect } from "next/navigation";
import { getServices } from "./lib/api";

export default async function DocsIndexPage() {
  const services = await getServices();

  // 自动跳转到第一个服务
  if (services.length > 0) {
    const first = services[0];
    redirect(`/docs/${encodeURIComponent(first.name)}/${encodeURIComponent(first.environment)}`);
  }

  // 无服务时显示空状态
  return (
    <div className="flex h-full flex-col items-center justify-center px-8 text-center">
      <p className="mb-4 font-mono text-[10px] font-semibold uppercase tracking-wider text-[var(--text-tertiary)]">
        暂无服务
      </p>
      <h1
        className="mb-3 text-2xl font-semibold text-[var(--text-primary)]"
        style={{ letterSpacing: "-0.03em" }}
      >
        暂无已注册服务
      </h1>
      <p className="max-w-[48ch] leading-7 text-[var(--text-secondary)]">
        启动 Center 服务端，并使用 Spring Boot Starter 适配器注册服务。已注册的服务将自动显示在侧边栏中。
      </p>
    </div>
  );
}
