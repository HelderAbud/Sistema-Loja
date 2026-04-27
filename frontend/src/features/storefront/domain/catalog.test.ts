import { describe, expect, it } from "vitest";
import { getProductBySlug } from "./catalog";

describe("storefront catalog (domain)", () => {
  it("retorna produto quando slug existe", () => {
    const product = getProductBySlug("tenis-urbano-lx");
    expect(product?.name).toBe("Tênis Urbano LX");
  });

  it("retorna null para slug inexistente", () => {
    expect(getProductBySlug("nao-existe")).toBeNull();
  });
});
