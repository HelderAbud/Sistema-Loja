import { defineConfig, devices } from "@playwright/test";

const host = "127.0.0.1";
const port = 3000;
const baseURL = `http://${host}:${port}`;

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL,
    trace: "on-first-retry",
    navigationTimeout: 60_000,
    actionTimeout: 15_000,
  },
  webServer: {
    command: `npm run build && npm run preview -- --host ${host} --port ${String(port)} --strictPort`,
    url: baseURL,
    reuseExistingServer: false,
    timeout: 180_000,
  },
  projects: [{ name: "msedge", use: { ...devices["Desktop Edge"], channel: "msedge" } }],
});
