/// <reference types="vitest/config" />
import react from "@vitejs/plugin-react";
import type { IncomingMessage, ServerResponse } from "http";
import path from "path";
import type { IndexHtmlTransformContext, Plugin } from "vite";
import { defineConfig } from "vite";

const API_TARGET = "http://localhost:8000";

/** Origens extra para connect-src (ex.: API absoluta em VITE_API_BASE), separadas por espaço. */
function cspConnectExtra(): string {
  return (process.env.VITE_CSP_CONNECT_SRC ?? "").trim();
}

function cspProduction(connectExtra: string): string {
  const connect = [
    "'self'",
    "https://fonts.googleapis.com",
    "https://fonts.gstatic.com",
    connectExtra,
  ]
    .filter(Boolean)
    .join(" ");
  return [
    "default-src 'self'",
    "base-uri 'self'",
    "frame-ancestors 'none'",
    "object-src 'none'",
    "script-src 'self'",
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com data:",
    "img-src 'self' data: blob:",
    `connect-src ${connect}`,
  ].join("; ");
}

function cspDevelopment(connectExtra: string): string {
  const connect = [
    "'self'",
    "ws:",
    "wss:",
    "http://127.0.0.1:8000",
    "http://localhost:8000",
    "https://fonts.googleapis.com",
    "https://fonts.gstatic.com",
    connectExtra,
  ]
    .filter(Boolean)
    .join(" ");
  return [
    "default-src 'self'",
    "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com data:",
    "img-src 'self' data: blob:",
    `connect-src ${connect}`,
  ].join("; ");
}

function lojappCspPlugin(): Plugin {
  const extra = cspConnectExtra();
  const devPolicy = cspDevelopment(extra);
  const prodPolicy = cspProduction(extra);
  return {
    name: "lojapp-csp",
    transformIndexHtml: {
      order: "post",
      handler(html: string, ctx: IndexHtmlTransformContext) {
        if (ctx.server) return html;
        const meta = `    <meta http-equiv="Content-Security-Policy" content="${prodPolicy}" />\n`;
        return html.replace("<head>", `<head>\n${meta}`);
      },
    },
    configureServer(server) {
      server.middlewares.use((_req, res, next) => {
        res.setHeader("Content-Security-Policy", devPolicy);
        next();
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), lojappCspPlugin()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/test/setup.ts",
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
  },
  server: {
    port: 3000,
    proxy: {
      "/api": {
        target: API_TARGET,
        changeOrigin: true,
        configure(proxy) {
          proxy.on("error", (err, req, res) => {
            const r = res as ServerResponse;
            if (!r || r.headersSent) return;
            const msg =
              err instanceof Error
                ? err.message
                : "Falha no proxy — confirme que a API Spring Boot está a correr (ex.: mvn spring-boot:run na porta 8000).";
            const path = (req as IncomingMessage & { url?: string }).url ?? "/api";
            r.writeHead(502, { "Content-Type": "application/json; charset=utf-8" });
            r.end(
              JSON.stringify({
                error: "BAD_GATEWAY",
                message: `${msg} (proxy → ${API_TARGET})`,
                path,
              }),
            );
          });
        },
      },
    },
  },
});
