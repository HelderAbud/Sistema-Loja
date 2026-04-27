import { expect, test, type Page } from "@playwright/test";

/** Sem API real: o refresh falha e o login devolve um access fictício (só memória SPA). */
async function mockAuthApi(page: Page) {
  await page.route("**/api/v1/auth/refresh", async (route) => {
    const method = route.request().method();
    if (method === "OPTIONS") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (method !== "POST") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: "{}",
    });
  });
  await page.route("**/api/v1/auth/login", async (route) => {
    const method = route.request().method();
    if (method === "OPTIONS") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (method !== "POST") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ accessToken: "e2e-mock-access-jwt" }),
    });
  });
  await page.route("**/api/v1/auth/logout", async (route) => {
    const method = route.request().method();
    if (method === "OPTIONS") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (method !== "POST") {
      await route.continue();
      return;
    }
    await route.fulfill({ status: 204 });
  });

  // Endpoints do painel usados após login nos cenários de sessão.
  await page.route("**/api/v1/lojapp/brands", async (route) => {
    const method = route.request().method();
    if (method === "OPTIONS") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (method !== "GET") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "[]",
    });
  });

  await page.route("**/api/v1/lojapp/products*", async (route) => {
    const method = route.request().method();
    if (method === "OPTIONS") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (method !== "GET") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
    });
  });
}

test.beforeEach(async ({ page }) => {
  await mockAuthApi(page);
});

async function waitForSessionBootstrap(page: Page) {
  await expect(page.getByText("A carregar sessão…")).toBeHidden({ timeout: 15_000 });
}

test("landing pública na raiz sem sessão", async ({ page }) => {
  await page.goto("/");
  await waitForSessionBootstrap(page);
  expect(new URL(page.url()).pathname).toBe("/");
  await expect(
    page.getByRole("heading", { name: /Operação comercial e fiscal alinhadas/i }),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: /Entrar no painel/i })).toBeVisible();
  await expect(page.getByRole("link", { name: /Explorar catálogo/i })).toBeVisible();
});

test("ecrã de login visível sem sessão", async ({ page }) => {
  await page.goto("/login");
  await waitForSessionBootstrap(page);
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("heading", { name: "LojApp", exact: true })).toBeVisible();
  await expect(page.getByLabel("Email")).toBeVisible();
  await expect(page.getByLabel("Palavra-passe")).toBeVisible();
});

test("rota do painel sem sessão redireciona para login", async ({ page }) => {
  await page.goto("/piloto/dashboard");
  await waitForSessionBootstrap(page);
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByLabel("Email")).toBeVisible();
});

test("login mockado abre o painel com separador Produtos", async ({ page }) => {
  await page.goto("/login");
  await waitForSessionBootstrap(page);
  await page.getByLabel("Email").fill("piloto-e2e@lojapp.test");
  await page.getByLabel("Palavra-passe").fill("senha1234");
  await page.getByRole("button", { name: /entrar na conta/i }).click();
  await expect(page).toHaveURL(/\/piloto\/products$/);
  await expect(page.getByRole("navigation", { name: "Secções do painel" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Produtos" })).toBeVisible();
  await expect(page.getByText("piloto-e2e@lojapp.test")).toBeVisible();
});

test("logout volta para login e protege rota privada", async ({ page }) => {
  await page.goto("/login");
  await waitForSessionBootstrap(page);
  await page.getByLabel("Email").fill("logout-e2e@lojapp.test");
  await page.getByLabel("Palavra-passe").fill("senha1234");
  await page.getByRole("button", { name: /entrar na conta/i }).click();
  await expect(page).toHaveURL(/\/piloto\/products$/);

  await page.getByRole("button", { name: /sair/i }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.goto("/piloto/dashboard");
  await expect(page).toHaveURL(/\/login$/);
});

test("login com erro 500 mostra mensagem e mantém utilizador no login", async ({ page }) => {
  await page.unroute("**/api/v1/auth/login");
  await page.route("**/api/v1/auth/login", async (route) => {
    const method = route.request().method();
    if (method === "OPTIONS") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (method !== "POST") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 500,
      contentType: "application/json",
      body: JSON.stringify({
        message: "Erro interno do servidor",
        code: "INTERNAL_ERROR",
        status: 500,
        path: "/api/v1/auth/login",
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.goto("/login");
  await waitForSessionBootstrap(page);
  await page.getByLabel("Email").fill("falha-login@lojapp.test");
  await page.getByLabel("Palavra-passe").fill("senha1234");
  await page.getByRole("button", { name: /entrar na conta/i }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(
    page.getByText(/servidor encontrou um erro|temporariamente indisponível/i),
  ).toBeVisible();
});

test("login com erro 401 mostra mensagem e não navega para rota privada", async ({ page }) => {
  await page.unroute("**/api/v1/auth/login");
  await page.route("**/api/v1/auth/login", async (route) => {
    const method = route.request().method();
    if (method === "OPTIONS") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (method !== "POST") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({
        message: "Credenciais inválidas",
        code: "UNAUTHORIZED",
        status: 401,
        path: "/api/v1/auth/login",
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.goto("/login");
  await waitForSessionBootstrap(page);
  await page.getByLabel("Email").fill("falha-401@lojapp.test");
  await page.getByLabel("Palavra-passe").fill("senha1234");
  await page.getByRole("button", { name: /entrar na conta/i }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText(/email ou palavra-passe incorretos|credenciais/i)).toBeVisible();
});

test("login com timeout de rede apresenta erro ao utilizador", async ({ page }) => {
  await page.unroute("**/api/v1/auth/login");
  await page.route("**/api/v1/auth/login", async (route) => {
    const method = route.request().method();
    if (method === "OPTIONS") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (method !== "POST") {
      await route.continue();
      return;
    }
    await route.abort("timedout");
  });

  await page.goto("/login");
  await waitForSessionBootstrap(page);
  await page.getByLabel("Email").fill("timeout@lojapp.test");
  await page.getByLabel("Palavra-passe").fill("senha1234");
  await page.getByRole("button", { name: /entrar na conta/i }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText(/sem ligação ao servidor/i)).toBeVisible();
});
