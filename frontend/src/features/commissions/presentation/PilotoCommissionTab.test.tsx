import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TestQueryProvider } from "@/test/queryWrapper";
import { PilotoCommissionTab } from "./PilotoCommissionTab";

const listCommissionAccruals = vi.fn();
const downloadCommissionAccrualsCsv = vi.fn();

vi.mock("@/api", () => ({
  listCommissionAccruals: (...args: unknown[]) => listCommissionAccruals(...args),
  downloadCommissionAccrualsCsv: (...args: unknown[]) => downloadCommissionAccrualsCsv(...args),
}));

describe("PilotoCommissionTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listCommissionAccruals.mockResolvedValue([
      {
        id: 5,
        saleId: 88,
        sellerId: 11,
        sellerName: "Ana",
        brandId: 8,
        brandName: "MarcaX",
        baseAmount: 20,
        percent: 12,
        amount: 2.4,
        createdAt: "2026-08-15T12:00:00Z",
      },
    ]);
  });

  it("mostra lançamentos e total", async () => {
    render(
      <TestQueryProvider>
        <PilotoCommissionTab />
      </TestQueryProvider>,
    );

    await waitFor(() => expect(listCommissionAccruals).toHaveBeenCalled());
    expect(await screen.findByText("Ana")).toBeInTheDocument();
    expect(screen.getByText(/total no período/i)).toHaveTextContent("2,40");
  });
});
