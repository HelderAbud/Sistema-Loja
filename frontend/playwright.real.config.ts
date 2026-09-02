import { defineConfig, devices } from "@playwright/test";

if (process.env.CI) {
  if (
    process.env.E2E_REAL_FLOW !== "1" ||
    !process.env.E2E_REAL_EMAIL ||
    !process.env.E2E_REAL_PASSWORD
  ) {
    throw new Error("CI e2e real exige E2E_REAL_FLOW=1, E2E_REAL_EMAIL e E2E_REAL_PASSWORD");
  }
}

const host = "127.0.0.1";
const port = 3000;
const baseURL = `http://${host}:${port}`;
const apiBase = (process.env.E2E_REAL_API_BASE ?? "http://127.0.0.1:8081").replace(/\/$/, "");

export default defineConfig({
  testDir: "./e2e",
  testMatch: "real-flow.spec.ts",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? "github" : "list",
  timeout: 120_000,
  use: {
    baseURL,
    trace: "on-first-retry",
    navigationTimeout: 60_000,
    actionTimeout: 20_000,
  },
  webServer: {
    command: `npm run build && npm run preview -- --host ${host} --port ${String(port)} --strictPort`,
    url: baseURL,
    reuseExistingServer: false,
    timeout: 180_000,
    env: {
      ...process.env,
      VITE_API_BASE: apiBase,
      VITE_CSP_CONNECT_SRC: apiBase,
    },
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
