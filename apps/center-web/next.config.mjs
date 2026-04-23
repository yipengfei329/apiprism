/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  devIndicators: false,
  async rewrites() {
    return [
      // Agent Doc 前端友好 URL → 内部 proxy
      { source: "/apidocs.md", destination: "/api/v1/apidocs.md" },
      { source: "/:service/:env/apidocs.md", destination: "/api/v1/:service/:env/apidocs.md" },
      { source: "/:service/:env/:groupSlug/apidocs.md", destination: "/api/v1/:service/:env/:groupSlug/apidocs.md" },
      { source: "/:service/:env/:groupSlug/:operationId/apidocs.md", destination: "/api/v1/:service/:env/:groupSlug/:operationId/apidocs.md" },
    ];
  },
};

export default nextConfig;
