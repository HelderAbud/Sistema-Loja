import { defineConfig, devices } from "@playwright/test";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const host = "127.0.0.1";
const port = Number(process.env.LOJAPP_CAPTURE_PORT ?? "3000");
const baseURL = `http://${host}:${port}`;

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const passCandidates = [
  process.env.LOJAPP_DIA12_PASS_FILE,
  "/tmp/lojapp-dia12-pass",
  path.resolve(__dirname, "../.scratch/dia12-pass"),
].filter(Boolean) as string[];

for (const p of passCandidates) {
  try {
    const pw = fs.readFileSync(p, "utf8").trim();
    if (pw) {
      process.env.LOJAPP_SCREENSHOT_PASSWORD = pw;
      break;
    }
  } catch {
    /* try next */
  }
}
process.env.LOJAPP_CAPTURE_PILOTO_DIA12 = "1";
if (!process.env.LOJAPP_SCREENSHOT_EMAIL) {
  process.env.LOJAPP_SCREENSHOT_EMAIL = "piloto-dia12@lojapp.demo";
}

export default defineConfig({
  testDir: "./e2e",
  testMatch: "capture-piloto-dia12.spec.ts",
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
  // Chrome instalado no Windows — evita download do Chromium (CDN costuma timeout).
  projects: [{ name: "chrome", use: { channel: "chrome" } }],
});
