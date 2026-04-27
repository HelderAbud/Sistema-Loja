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
});
