import { cache } from "react";

const apiBase = process.env.NEXT_PUBLIC_APIPRISM_API_BASE ?? "http://localhost:8080";

// ---- 类型定义 ----

export type ServiceCatalogItem = {
  name: string;
  environment: string;
  title: string;
  version: string;
  updatedAt: string;
  groups: string[];
};

/** 标准 JSON Schema 子集类型 */
export type JsonSchema = {
  type?: string;
  format?: string;
  description?: string;
  properties?: Record<string, JsonSchema>;
  items?: JsonSchema;
  required?: string[];
  enum?: unknown[];
  example?: unknown;
  deprecated?: boolean;
  $circular?: boolean;
};

export type CanonicalParameter = {
  name: string;
  location: string;
  required: boolean;
  schema: JsonSchema | null;
  description: string;
};

export type CanonicalRequestBody = {
  required: boolean;
  contentType: string;
  schema: JsonSchema | null;
};

export type CanonicalResponse = {
  statusCode: string;
  description: string;
  contentType: string;
  schema: JsonSchema | null;
};

export type CanonicalOperation = {
  operationId: string;
  method: string;
  path: string;
  summary: string;
  description: string;
  tags: string[];
  securityRequirements: string[];
  parameters: CanonicalParameter[];
  requestBody: CanonicalRequestBody | null;
  responses: CanonicalResponse[];
};

export type CanonicalGroup = {
  name: string;
  description: string;
  operations: CanonicalOperation[];
};

export type ServiceRef = {
  name: string;
  environment: string;
};

export type CanonicalServiceSnapshot = {
  ref: ServiceRef;
  title: string;
  version: string;
  serverUrls: string[];
  groups: CanonicalGroup[];
  updatedAt: string;
};

// ---- Fetch 工具函数 ----

// React.cache() 保证同一服务端渲染过程中多个组件调用时只发出一次 HTTP 请求
export const getServices = cache(async (): Promise<ServiceCatalogItem[]> => {
  try {
    const res = await fetch(`${apiBase}/api/v1/services`, { cache: "no-store" });
    if (!res.ok) return [];
    return res.json();
  } catch {
    return [];
  }
});

export async function getServiceSnapshot(
  service: string,
  environment: string
): Promise<CanonicalServiceSnapshot | null> {
  try {
    const res = await fetch(
      `${apiBase}/api/v1/services/${encodeURIComponent(service)}/environments/${encodeURIComponent(environment)}`,
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

export async function getGroup(
  service: string,
  environment: string,
  group: string
): Promise<CanonicalGroup | null> {
  try {
    const res = await fetch(
      `${apiBase}/api/v1/services/${encodeURIComponent(service)}/environments/${encodeURIComponent(environment)}/groups/${encodeURIComponent(group)}`,
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

export async function getOperation(
  service: string,
  environment: string,
  operationId: string
): Promise<CanonicalOperation | null> {
  try {
    const res = await fetch(
      `${apiBase}/api/v1/services/${encodeURIComponent(service)}/environments/${encodeURIComponent(environment)}/operations/${encodeURIComponent(operationId)}`,
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

export async function getAgentMarkdown(
  service: string,
  environment: string,
  operationId: string
): Promise<string | null> {
  try {
    const res = await fetch(
      `${apiBase}/api/v1/agent-markdown/services/${encodeURIComponent(service)}/environments/${encodeURIComponent(environment)}/operations/${encodeURIComponent(operationId)}`,
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.text();
  } catch {
    return null;
  }
}
