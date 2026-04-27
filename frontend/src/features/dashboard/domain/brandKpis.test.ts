import { describe, expect, it } from "vitest";
import { computeBrandKpiSummary } from "./brandKpis";

describe("computeBrandKpiSummary", () => {
  it("agrega métricas por marca e ABC", () => {
    const summary = computeBrandKpiSummary(
      {
        from: "a",
        to: "b",
        totalBrands: 1,
        brandLimit: 50,
        brandOffset: 0,
        metrics: [
          {
            brand: "X",
            faturamento: 100,
            lucro: 30,
            quantidadeVendida: 2,
            margem: 30,
            giro: "1",
            insight: "",
          },
        ],
      },
      { from: "a", to: "b", totalRevenue: 80, rows: [] },
    );
    expect(summary.faturamento).toBe(100);
    expect(summary.lucro).toBe(30);
    expect(summary.unitsSold).toBe(2);
    expect(summary.ticketMedio).toBe(50);
    expect(summary.abcRevenue).toBe(80);
  });
});
