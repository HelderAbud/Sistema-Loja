import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TestQueryProvider } from "@/test/queryWrapper";
import { PilotoSaleTab } from "./PilotoSaleTab";

const listProducts = vi.fn();
const getProductStock = vi.fn();
const registerSale = vi.fn();

vi.mock("@/api", () => ({
  listProducts: (...args: unknown[]) => listProducts(...args),
  getProductStock: (...args: unknown[]) => getProductStock(...args),
  registerSale: (...args: unknown[]) => registerSale(...args),
}));

const mockProduct = {
  id: 42,
  name: "Camiseta Teste",
  brandName: "MarcaX",
  ean: null,
  ncm: null,
  sku: null,
  costPrice: 10,
  salePrice: 25,
  minimumStock: 0,
};

describe("PilotoSaleTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listProducts.mockResolvedValue({
      content: [mockProduct],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
    });
    getProductStock.mockResolvedValue({ quantity: 2 });
    registerSale.mockResolvedValue({
      id: 99,
      productId: 42,
      quantity: 1,
      unitPrice: 20,
      unitCost: 10,
      soldAt: "2026-01-01T00:00:00Z",
    });
  });

  it("desativa o submit e mostra alerta quando a quantidade excede o saldo", async () => {
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    fireEvent.change(screen.getByPlaceholderText(/camiseta/i), { target: { value: "c" } });
    await waitFor(() => expect(listProducts).toHaveBeenCalled());

    const option = await screen.findByRole("button", { name: /#42 — Camiseta Teste/ });
    fireEvent.mouseDown(option);
    fireEvent.click(option);

    await waitFor(() => expect(getProductStock).toHaveBeenCalledWith(42));

    fireEvent.change(screen.getByLabelText(/^quantidade$/i), { target: { value: "5" } });

    expect(screen.getByRole("alert")).toHaveTextContent(/maior que o saldo/);
    expect(screen.getByRole("button", { name: /registar venda/i })).toBeDisabled();
  });

  it("mantém o submit ativo quando a quantidade não excede o saldo", async () => {
    getProductStock.mockResolvedValue({ quantity: 10 });
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    fireEvent.change(screen.getByPlaceholderText(/camiseta/i), { target: { value: "c" } });
    await waitFor(() => expect(listProducts).toHaveBeenCalled());

    const option = await screen.findByRole("button", { name: /#42 — Camiseta Teste/ });
    fireEvent.mouseDown(option);
    fireEvent.click(option);

    await waitFor(() => expect(getProductStock).toHaveBeenCalledWith(42));

    fireEvent.change(screen.getByLabelText(/^quantidade$/i), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText(/preço de venda unitário/i), {
      target: { value: "18" },
    });

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /registar venda/i })).not.toBeDisabled();
  });
});
