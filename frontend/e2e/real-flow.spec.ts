import { expect, test, type APIRequestContext } from "@playwright/test";

type AccessTokenResponse = { accessToken: string };
type BrandResponse = { id: number; name: string };
type ProductResponse = {
  id: number;
  name: string;
  brandName: string;
  costPrice: number;
  salePrice: number;
};

const ENABLED = process.env.E2E_REAL_FLOW === "1";
const E2E_EMAIL = process.env.E2E_REAL_EMAIL ?? "";
const E2E_PASSWORD = process.env.E2E_REAL_PASSWORD ?? "";
const API_BASE = process.env.E2E_REAL_API_BASE ?? "http://localhost:8000";

async function apiLoginAccessToken(request: APIRequestContext): Promise<string> {
  const resp = await request.post(`${API_BASE}/api/v1/auth/login`, {
    data: { email: E2E_EMAIL, password: E2E_PASSWORD },
  });
  expect(resp.ok()).toBeTruthy();
  const body = (await resp.json()) as AccessTokenResponse;
  expect(Boolean(body.accessToken)).toBeTruthy();
  return body.accessToken;
}

async function createProductWithStock(
  request: APIRequestContext,
  bearerToken: string,
): Promise<ProductResponse> {
  const authz = { Authorization: `Bearer ${bearerToken}` };
  const suffix = Date.now().toString();

  const brandResp = await request.post(`${API_BASE}/api/v1/lojapp/brands`, {
    headers: authz,
    data: { name: `Marca E2E Real ${suffix}` },
  });
  expect(brandResp.ok()).toBeTruthy();
  const brand = (await brandResp.json()) as BrandResponse;

  const productResp = await request.post(`${API_BASE}/api/v1/lojapp/products`, {
    headers: authz,
    data: {
      name: `Produto E2E Real ${suffix}`,
      brandId: brand.id,
      ean: null,
      ncm: null,
      sku: `E2E-${suffix}`,
      costPrice: 10.5,
      salePrice: 19.9,
      minimumStock: 1,
    },
  });
  expect(productResp.ok()).toBeTruthy();
  const product = (await productResp.json()) as ProductResponse;

  const adjustResp = await request.post(`${API_BASE}/api/v1/lojapp/inventory/adjust`, {
    headers: authz,
    data: {
      productId: product.id,
      quantity: 5,
      reason: "AJUSTE_E2E_REAL_FLOW",
    },
  });
  expect(adjustResp.ok()).toBeTruthy();

  return product;
}

test.describe("jornada real (API + UI, sem mocks)", () => {
  test.skip(!ENABLED, "Defina E2E_REAL_FLOW=1 para correr contra API real");
  test.skip(
    !E2E_EMAIL || !E2E_PASSWORD,
    "Defina E2E_REAL_EMAIL e E2E_REAL_PASSWORD (conta demo local)",
  );

  test("login -> nova venda -> dashboard", async ({ page, request }) => {
    const token = await apiLoginAccessToken(request);
    const product = await createProductWithStock(request, token);

    await page.goto("/login");
    await expect(page.getByText("A carregar sessão…")).toBeHidden({ timeout: 15_000 });

    await page.getByLabel("Email").fill(E2E_EMAIL);
    await page.getByLabel("Palavra-passe").fill(E2E_PASSWORD);
    await page.getByRole("button", { name: /entrar na conta/i }).click();
    await expect(page).toHaveURL(/\/piloto\/products$/);

    await page.getByRole("button", { name: "Nova venda" }).click();
    await expect(page).toHaveURL(/\/piloto\/sale$/);

    const productInput = page.getByLabel("Produto — pesquisar por nome");
    await productInput.fill(product.name);
    const pickProduct = page.getByRole("button", { name: new RegExp(`#${product.id}`) });
    await expect(pickProduct.first()).toBeVisible({ timeout: 10_000 });
    await pickProduct.first().click();

    await page.getByLabel("Quantidade").fill("1");
    await page.getByLabel(/Preço de venda unitário/).fill("19.9");
    await page.getByRole("button", { name: /registar venda/i }).click();
    await expect(page.getByText(/Venda registada — id/i)).toBeVisible();

    await page.getByRole("button", { name: "Dashboard" }).click();
    await expect(page).toHaveURL(/\/piloto\/dashboard$/);
    await expect(page.getByRole("heading", { name: /Dashboard executivo/i })).toBeVisible();
  });
});
