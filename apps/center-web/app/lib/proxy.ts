import "server-only";

const DEFAULT_INTERNAL_API_BASE = "http://127.0.0.1:8080";
const HOP_BY_HOP_HEADERS = [
  "connection",
  "content-length",
  "host",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
];

function getInternalApiBase(): string {
  return process.env.APIPRISM_SERVER_INTERNAL_BASE ?? DEFAULT_INTERNAL_API_BASE;
}

function stripTrailingSlash(value: string): string {
  return value.endsWith("/") ? value.slice(0, -1) : value;
}

function buildUpstreamUrl(prefix: string, pathSegments: string[], search: string): string {
  const normalizedPrefix = prefix.startsWith("/") ? prefix : `/${prefix}`;
  const suffix = pathSegments.length > 0 ? `/${pathSegments.join("/")}` : "";
  return `${stripTrailingSlash(getInternalApiBase())}${normalizedPrefix}${suffix}${search}`;
}

export async function proxyToInternalApi(
  request: Request,
  prefix: string,
  pathSegments: string[]
): Promise<Response> {
  const url = new URL(request.url);
  const upstreamUrl = buildUpstreamUrl(prefix, pathSegments, url.search);
  const headers = new Headers(request.headers);

  HOP_BY_HOP_HEADERS.forEach((header) => headers.delete(header));

  // 将前端 origin 传递给后端，确保后端生成的链接指向前端而非后端
  const origin = new URL(request.url);
  headers.set("X-Forwarded-Host", origin.host);
  headers.set("X-Forwarded-Proto", origin.protocol.replace(":", ""));

  const hasBody = request.method !== "GET" && request.method !== "HEAD" && request.method !== "DELETE";
  const body = hasBody ? await request.arrayBuffer() : undefined;
  const upstreamResponse = await fetch(upstreamUrl, {
    method: request.method,
    headers,
    body,
    cache: "no-store",
    redirect: "manual",
  });

  const responseHeaders = new Headers(upstreamResponse.headers);
  HOP_BY_HOP_HEADERS.forEach((header) => responseHeaders.delete(header));

  return new Response(upstreamResponse.body, {
    status: upstreamResponse.status,
    statusText: upstreamResponse.statusText,
    headers: responseHeaders,
  });
}
