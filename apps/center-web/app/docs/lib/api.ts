import { cache } from "react";
import { getInternalApiUrl } from "@/app/lib/internal-api";

// ---- 类型定义 ----

export type GroupRef = {
  name: string;
  slug: string;
};

export type ServiceCatalogItem = {
  name: string;
  environment: string;
  title: string;
  version: string;
  updatedAt: string;
  groups: GroupRef[];
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

export type CanonicalOAuthFlow = {
  flowType: string;
  authorizationUrl: string | null;
  tokenUrl: string | null;
  refreshUrl: string | null;
  scopes: Record<string, string>;
};

export type CanonicalSecurityScheme = {
  type: string | null;
  scheme: string | null;
  bearerFormat: string | null;
  in: string | null;
  paramName: string | null;
  openIdConnectUrl: string | null;
  description: string | null;
  oauthFlows: CanonicalOAuthFlow[];
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
  slug: string;
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
  securitySchemes: Record<string, CanonicalSecurityScheme>;
  updatedAt: string;
};

// ---- Fetch 工具函数 ----

// React.cache() 保证同一服务端渲染过程中多个组件调用时只发出一次 HTTP 请求
export const getServices = cache(async (): Promise<ServiceCatalogItem[]> => {
  try {
    const res = await fetch(getInternalApiUrl("/api/v1/services"), { cache: "no-store" });
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
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}`
      ),
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
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/groups/${encodeURIComponent(group)}`
      ),
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
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/operations/${encodeURIComponent(operationId)}`
      ),
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

// ---- MCP 配置 ----

export type McpToggleResponse = {
  enabled: boolean;
  sseEndpoint: string | null;
  streamableEndpoint: string | null;
};

export type McpStatusResponse = {
  serviceEnabled: boolean;
  sseEndpoint: string | null;
  streamableEndpoint: string | null;
  enabledGroups: string[];
};

export async function getMcpServiceStatus(
  service: string,
  environment: string
): Promise<McpStatusResponse | null> {
  try {
    const res = await fetch(
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/mcp-config`
      ),
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

// ---- 版本历史 ----

export type RevisionSummary = {
  id: string;
  serviceName: string;
  environment: string;
  seq: number;
  specHash: string;
  title: string | null;
  version: string | null;
  adapterType: string | null;
  source: string;
  warningsCount: number;
  registeredAt: string;
  current: boolean;
  endpointCount: number | null;
  addedCount: number | null;
  removedCount: number | null;
  modifiedCount: number | null;
};

export async function listRevisions(
  service: string,
  environment: string
): Promise<RevisionSummary[]> {
  try {
    const res = await fetch(
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/rev`
      ),
      { cache: "no-store" }
    );
    if (!res.ok) return [];
    return res.json();
  } catch {
    return [];
  }
}

export async function getRevisionSnapshot(
  service: string,
  environment: string,
  revisionId: string
): Promise<CanonicalServiceSnapshot | null> {
  try {
    const res = await fetch(
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/rev/${encodeURIComponent(revisionId)}`
      ),
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

export async function getRevisionGroup(
  service: string,
  environment: string,
  revisionId: string,
  group: string
): Promise<CanonicalGroup | null> {
  try {
    const res = await fetch(
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/rev/${encodeURIComponent(revisionId)}/groups/${encodeURIComponent(group)}`
      ),
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

export async function getRevisionOperation(
  service: string,
  environment: string,
  revisionId: string,
  operationId: string
): Promise<CanonicalOperation | null> {
  try {
    const res = await fetch(
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/rev/${encodeURIComponent(revisionId)}/operations/${encodeURIComponent(operationId)}`
      ),
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

export async function getMcpGroupStatus(
  service: string,
  environment: string,
  group: string
): Promise<McpToggleResponse | null> {
  try {
    const res = await fetch(
      getInternalApiUrl(
        `/api/v1/services/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(group)}/mcp-config`
      ),
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

