import { proxyToInternalApi } from "@/app/lib/proxy";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type RouteContext = {
  params: Promise<{ path: string[] }>;
};

async function handle(request: Request, context: RouteContext): Promise<Response> {
  const { path } = await context.params;
  return proxyToInternalApi(request, "/actuator", path);
}

export const GET = handle;
export const HEAD = handle;
