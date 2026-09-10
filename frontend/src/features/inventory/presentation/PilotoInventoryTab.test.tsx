import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { TestQueryProvider } from "@/test/queryWrapper";
import { PilotoInventoryTab } from "./PilotoInventoryTab";

const { listLowStockMock, listProductMovementsMock, adjustStockMock } = vi.hoisted(() => ({
  listLowStockMock: vi.fn(),
  listProductMovementsMock: vi.fn(),
  adjustStockMock: vi.fn(),
}));

vi.mock("@/hooks", () => ({
  useCurrentUser: () => ({ data: { appRole: "USER" } }),
}));

vi.mock("@/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api")>();
  return {
    ...actual,
    listLowStock: listLowStockMock,
    listProductMovements: listProductMovementsMock,
    adjustStock: adjustStockMock,
  };
});

describe("PilotoInventoryTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listLowStockMock.mockResolvedValue([]);
    listProductMovementsMock.mockResolvedValue({
      content: [
        {
          id: 1,
          movementType: "SALE",
          quantity: -3,
          source: "SALE_REGISTER",
          sourceId: 456,
          reason: null,
          createdAt: "2026-09-10T14:32:00.000Z",
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
    });
  });

  it("carrega o kardex ao indicar o id do produto", async () => {
    render(
      <TestQueryProvider>
        <PilotoInventoryTab />
      </TestQueryProvider>,
    );

    expect(screen.getByText(/histórico de movimentos/i)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/id do produto para histórico/i), {
      target: { value: "12" },
    });

    await waitFor(() => expect(listProductMovementsMock).toHaveBeenCalledWith(12, 0));
    expect(await screen.findByText("Venda")).toBeInTheDocument();
    expect(screen.getByText("SALE_REGISTER #456")).toBeInTheDocument();
    expect(screen.getByText("-3")).toBeInTheDocument();
  });
});
