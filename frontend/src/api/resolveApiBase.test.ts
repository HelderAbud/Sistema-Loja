import { describe, expect, it } from "vitest";
import { resolveApiBase } from "./resolveApiBase";

const RAILWAY = "https://sistema-loja-production-7608.up.railway.app";

describe("resolveApiBase", () => {
  it("na demo pública usa o próprio site para o cookie de refresh", () => {
    expect(resolveApiBase(RAILWAY, "sistema-loja-psi.vercel.app")).toBe("");
    expect(resolveApiBase(`${RAILWAY}/`, "sistema-loja-psi.vercel.app")).toBe("");
  });

  it("mantém a API absoluta fora da demo", () => {
    expect(resolveApiBase(RAILWAY, "localhost:5173")).toBe(RAILWAY);
    expect(resolveApiBase("http://localhost:8081", "localhost:5173")).toBe("http://localhost:8081");
  });

  it("sem configuração fica relativo", () => {
    expect(resolveApiBase(undefined, "sistema-loja-psi.vercel.app")).toBe("");
    expect(resolveApiBase("  ", "localhost")).toBe("");
  });
});
