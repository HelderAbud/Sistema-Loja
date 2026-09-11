import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { InventoryKpiSection } from "./InventoryKpiSection";

describe("InventoryKpiSection", () => {
  it("mostra valor ao custo do estoque", () => {
    render(
      <InventoryKpiSection
        inv={{
          totalSkus: 4,
          totalUnits: 12,
          lowStockCount: 1,
          skusWithPositiveStock: 3,
          totalStockValue: 240,
        }}
      />,
    );
    expect(screen.getByText(/valor ao custo/i)).toBeInTheDocument();
    expect(screen.getByText(/R\$\s*240,00/)).toBeInTheDocument();
  });
});
