import "server-only";

const DEFAULT_INTERNAL_API_BASE = "http://127.0.0.1:8080";

function normalizePath(path: string): string {
  return path.startsWith("/") ? path : `/${path}`;
}

export function getInternalApiUrl(path: string): string {
  const apiBase = process.env.APIPRISM_SERVER_INTERNAL_BASE ?? DEFAULT_INTERNAL_API_BASE;
  return `${apiBase}${normalizePath(path)}`;
}
