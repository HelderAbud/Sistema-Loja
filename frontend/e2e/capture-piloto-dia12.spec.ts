import { expect, test } from "@playwright/test";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const ENABLED = process.env.LOJAPP_CAPTURE_PILOTO_DIA12 === "1";
const EMAIL = process.env.LOJAPP_SCREENSHOT_EMAIL ?? "piloto-dia12@lojapp.demo";

function resolvePassword(): string {
  if (process.env.LOJAPP_SCREENSHOT_PASSWORD) {
    return process.env.LOJAPP_SCREENSHOT_PASSWORD;
  }
  const passFile = process.env.LOJAPP_DIA12_PASS_FILE ?? "/tmp/lojapp-dia12-pass";
  try {
    return fs.readFileSync(passFile, "utf8").trim();
  } catch {
    return "";
  }
}

const PASSWORD = resolvePassword();

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT_DIR = path.resolve(__dirname, "../../docs/screenshots/piloto");

test.describe("captura piloto Dia 12", () => {
  test.skip(!ENABLED, "Defina LOJAPP_CAPTURE_PILOTO_DIA12=1");
  test.skip(!PASSWORD, "Defina LOJAPP_SCREENSHOT_PASSWORD");

  test("gera 01–03 em docs/screenshots/piloto/", async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 });

    async function snap(file: string) {
      await page.screenshot({ path: path.join(OUT_DIR, file), fullPage: false });
    }

    async function waitBootstrap() {
      await expect(page.getByText("A carregar sessão…")).toBeHidden({ timeout: 20_000 });
    }

    await page.goto("/login");
    await waitBootstrap();
    await page.getByLabel("Email").fill(EMAIL);
    await page.getByLabel("Palavra-passe").fill(PASSWORD);
    await page.getByRole("button", { name: /entrar na conta/i }).click();
    await expect(page).toHaveURL(/\/piloto\//, { timeout: 30_000 });
    await waitBootstrap();

    await page.goto("/piloto/inventory");
    await waitBootstrap();
    await expect(page.getByRole("tablist", { name: "Secções do painel" })).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(800);
    await snap("01-estoque-pos-nfe.png");

    await page.goto("/piloto/sales");
    await waitBootstrap();
    await page.waitForTimeout(800);
    await snap("02-venda.png");

    await page.goto("/piloto/inventory");
    await waitBootstrap();
    await page.waitForTimeout(800);
    await snap("03-estoque-pos-venda.png");
  });
});
