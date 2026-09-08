import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { TestQueryProvider } from "@/test/queryWrapper";
import { OrdersPage } from "./OrdersPage";

vi.mock("../../api", () => ({
  listBrands: vi.fn().mockResolvedValue([]),
  listSales: vi.fn().mockResolvedValue({
    content: [],
    totalElements: 0,
    totalPages: 0,
    size: 25,
    number: 0,
    first: true,
    last: true,
  }),
  summarizeSales: vi.fn().mockResolvedValue({
    revenue: 0,
    unitsSold: 0,
    averageTicket: 0,
  }),
  summarizeSalesDaily: vi.fn().mockResolvedValue([]),
}));

describe("OrdersPage", () => {
  it("renderiza o histórico sem ReferenceError de summaryTemplate", () => {
    render(
      <MemoryRouter>
        <TestQueryProvider>
          <OrdersPage />
        </TestQueryProvider>
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: /histórico de pedidos/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /aplicar filtros/i })).toBeInTheDocument();
  });
});
