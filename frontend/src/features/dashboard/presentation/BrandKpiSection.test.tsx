import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { BrandKpiSection } from "./BrandKpiSection";

const dashboard = {
  from: "2026-10-01",
  to: "2026-10-24",
  totalBrands: 1,
  brandLimit: 50,
  brandOffset: 0,
  metrics: [
    {
      brand: "Ogochi",
      faturamento: 200,
      lucro: 50,
      quantidadeVendida: 4,
      margem: 25,
      giro: "1",
      insight: "",
    },
  ],
};

describe("BrandKpiSection", () => {
  it("mostra faturamento, lucro, ticket, unidades, margem e stock", () => {
    render(
      <BrandKpiSection
        data={dashboard}
        abc={{ from: "2026-10-01", to: "2026-10-24", totalRevenue: 180, rows: [] }}
        inv={{
          totalSkus: 12,
          totalUnits: 40,
          lowStockCount: 3,
          skusWithPositiveStock: 10,
          totalStockValue: 900,
        }}
        inventoryPending={false}
      />,
    );

    expect(screen.getByText("2026-10-01")).toBeInTheDocument();
    expect(screen.getByText("2026-10-24")).toBeInTheDocument();
    expect(screen.getByText(/R\$\s*200,00/)).toBeInTheDocument();
    expect(screen.getAllByText(/R\$\s*50,00/)).toHaveLength(2);
    expect(screen.getByText(/R\$\s*180,00/)).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
    expect(screen.getByText("3 / 12")).toBeInTheDocument();
    expect(screen.getByText("25.0 %")).toBeInTheDocument();
  });

  it("não mostra stock enquanto o inventário ainda carrega", () => {
    render(<BrandKpiSection data={dashboard} inventoryPending />);

    expect(screen.queryByText("3 / 12")).not.toBeInTheDocument();
    expect(document.querySelector(".skeleton-block")).not.toBeNull();
  });
});
