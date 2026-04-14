const PUBLIC_API_BASE = process.env.NEXT_PUBLIC_APIPRISM_API_BASE ?? "";

function normalizePath(path: string): string {
  return path.startsWith("/") ? path : `/${path}`;
}

export function getPublicApiUrl(path: string): string {
  return `${PUBLIC_API_BASE}${normalizePath(path)}`;
}
