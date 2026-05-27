import { defineConfig, devices } from "@playwright/test";

/** Captura de screenshots do portfólio — requer `npm run dev` (ou preview) já a correr em :3000. */
const host = "127.0.0.1";
const port = Number(process.env.LOJAPP_CAPTURE_PORT ?? "3000");
const baseURL = `http://${host}:${port}`;

export default defineConfig({
  testDir: "./e2e",
  testMatch: "capture-portfolio-screenshots.spec.ts",
  fullyParallel: false,
  workers: 1,
  reporter: "list",
  use: {
    baseURL,
    ...devices["Desktop Chrome"],
    viewport: { width: 1280, height: 800 },
    screenshot: "off",
    navigationTimeout: 60_000,
    actionTimeout: 20_000,
  },
  webServer: {
    command: `npm run dev -- --host ${host} --port ${String(port)} --strictPort`,
    url: baseURL,
    reuseExistingServer: true,
    timeout: 120_000,
  },
  projects: [{ name: "chromium", use: { channel: "chromium" } }],
});
