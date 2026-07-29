import { expect, test } from "@playwright/test";
import path from "path";
import { fileURLToPath } from "url";

const ENABLED = process.env.LOJAPP_CAPTURE_SCREENSHOTS === "1";
const EMAIL = process.env.LOJAPP_SCREENSHOT_EMAIL ?? "piloto@lojapp.demo";
const PASSWORD = process.env.LOJAPP_SCREENSHOT_PASSWORD ?? "";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT_DIR = path.resolve(__dirname, "../../docs/screenshots");

test.describe("captura portfólio A1", () => {
  test.skip(!ENABLED, "Defina LOJAPP_CAPTURE_SCREENSHOTS=1");
  test.skip(!PASSWORD, "Defina LOJAPP_SCREENSHOT_PASSWORD (conta demo com dados)");

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 });
  });

  test("gera PNG 01–06 em docs/screenshots/", async ({ page }) => {
    async function snap(file: string) {
      const full = path.join(OUT_DIR, file);
      await page.screenshot({ path: full, fullPage: false });
    }

    async function waitBootstrap() {
      await expect(page.getByText("A carregar sessão…")).toBeHidden({ timeout: 20_000 });
    }

    async function login() {
      await page.goto("/login");
      await waitBootstrap();
      await snap("01-login.png");
      await page.getByLabel("Email").fill(EMAIL);
      await page.getByLabel("Palavra-passe").fill(PASSWORD);
      await page.getByRole("button", { name: /entrar na conta/i }).click();
      await expect(page).toHaveURL(/\/piloto\//, { timeout: 30_000 });
      await waitBootstrap();
    }

    async function openTab(tabPath: string) {
      await page.goto(tabPath);
      await waitBootstrap();
      await expect(page.getByRole("tablist", { name: "Secções do painel" })).toBeVisible({
        timeout: 30_000,
      });
      await page.waitForTimeout(800);
    }

    await login();

    await openTab("/piloto/dashboard");
    await expect(page.getByRole("button", { name: "Dashboard" })).toHaveClass(/active/);
    await snap("02-dashboard.png");

    await openTab("/piloto/sales");
    await snap("03-vendas.png");

    await openTab("/piloto/inventory");
    await snap("04-estoque.png");

    await openTab("/piloto/nfe");
    await snap("05-importacao-xml.png");

    await openTab("/piloto/brands");
    await snap("06-relatorios.png");
  });
});
