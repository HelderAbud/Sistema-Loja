import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiJson } from "./api";

describe("apiJson", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.reject(new Error("fetch não configurado no teste"))),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("inclui code e message no Error quando a API devolve ApiErrorResponse", async () => {
    vi.mocked(fetch).mockResolvedValue({
      ok: false,
      status: 400,
      text: async () =>
        JSON.stringify({
          message: "Stock insuficiente para a quantidade pedida",
          code: "BAD_REQUEST",
          timestamp: "2026-04-24T12:00:00Z",
        }),
    } as Response);

    await expect(apiJson("/api/v1/x", { method: "GET" }, true)).rejects.toThrow(
      "Stock insuficiente para a quantidade pedida",
    );
  });

  it("aceita campo legado error em vez de code", async () => {
    vi.mocked(fetch).mockResolvedValue({
      ok: false,
      status: 409,
      text: async () =>
        JSON.stringify({
          message: "Conflito",
          error: "CONFLICT",
          timestamp: "2026-04-24T12:00:00Z",
        }),
    } as Response);

    await expect(apiJson("/api/v1/x", {}, true)).rejects.toThrow("Conflito");
  });

  it("partilha um único POST /auth/refresh quando dois pedidos recebem 401 em paralelo", async () => {
    const { useAuthStore } = await import("./authStore");
    useAuthStore.getState().setAccessToken("expired-at");

    const refreshHits: string[] = [];
    const attempts: Record<string, number> = {};

    vi.mocked(fetch).mockImplementation(async (input) => {
      const href = String(input);
      if (href.includes("/api/v1/auth/refresh")) {
        refreshHits.push(href);
        await new Promise((r) => setTimeout(r, 40));
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify({ accessToken: "rotated-at" }),
        } as Response;
      }
      attempts[href] = (attempts[href] ?? 0) + 1;
      if (attempts[href] === 1) {
        return { ok: false, status: 401, text: async () => "" } as Response;
      }
      return {
        ok: true,
        status: 200,
        text: async () => JSON.stringify({ ok: true, path: href }),
      } as Response;
    });

    const [a, b] = await Promise.all([
      apiJson<{ ok: boolean }>("/api/v1/lojapp/dashboard/brands"),
      apiJson<{ ok: boolean }>("/api/v1/lojapp/dashboard/inventory-kpis"),
    ]);

    expect(a.ok).toBe(true);
    expect(b.ok).toBe(true);
    expect(refreshHits).toHaveLength(1);
    expect(useAuthStore.getState().accessToken).toBe("rotated-at");
  });
});
