import { render, screen, waitFor, fireEvent, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TestQueryProvider } from "@/test/queryWrapper";
import { PilotoSaleTab } from "./PilotoSaleTab";

const listProducts = vi.fn();
const getProductStock = vi.fn();
const finalizePosSale = vi.fn();
const listSellers = vi.fn();
const getCurrentCashSession = vi.fn();
const openCashSession = vi.fn();
const closeCashSession = vi.fn();
const getCloseCashSessionPreview = vi.fn();

vi.mock("@/api", () => ({
  listProducts: (...args: unknown[]) => listProducts(...args),
  getProductStock: (...args: unknown[]) => getProductStock(...args),
  finalizePosSale: (...args: unknown[]) => finalizePosSale(...args),
  listSellers: (...args: unknown[]) => listSellers(...args),
  getCurrentCashSession: (...args: unknown[]) => getCurrentCashSession(...args),
  openCashSession: (...args: unknown[]) => openCashSession(...args),
  closeCashSession: (...args: unknown[]) => closeCashSession(...args),
  getCloseCashSessionPreview: (...args: unknown[]) => getCloseCashSessionPreview(...args),
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

const openCash = {
  open: true,
  cashSessionId: 7,
  openingAmount: 0,
  openedAt: "2026-09-09T12:00:00Z",
  expectedAmount: 0,
  expectedCashAmount: 0,
  expectedCardAmount: 0,
  expectedPixAmount: 0,
};

async function pickTestProduct() {
  await waitFor(() => expect(screen.getByText(/turno de caixa #7/i)).toBeInTheDocument());
  fireEvent.change(screen.getByPlaceholderText(/camiseta/i), { target: { value: "c" } });
  const option = await screen.findByRole("button", { name: /#42 — Camiseta Teste/ });
  fireEvent.mouseDown(option);
  fireEvent.click(option);
  await waitFor(() => expect(getProductStock).toHaveBeenCalledWith(42));
}

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
    listSellers.mockResolvedValue([
      { id: 11, displayName: "Ana", active: true, sortOrder: 0, createdAt: "2026-01-01T00:00:00Z" },
    ]);
    getProductStock.mockResolvedValue({ quantity: 2 });
    getCurrentCashSession.mockResolvedValue(openCash);
    openCashSession.mockResolvedValue({
      cashSessionId: 7,
      openingAmount: 0,
      openedAt: "2026-09-09T12:00:00Z",
      status: "OPEN",
    });
    closeCashSession.mockResolvedValue({
      cashSessionId: 7,
      expectedAmount: 0,
      countedAmount: 0,
      differenceAmount: 0,
      closedAt: "2026-09-09T13:00:00Z",
      status: "CLOSED",
    });
    getCloseCashSessionPreview.mockResolvedValue({
      cashSessionId: 7,
      expectedAmount: 0,
      expectedCashAmount: 0,
      expectedCardAmount: 0,
      expectedPixAmount: 0,
      countedAmount: 0,
      differenceAmount: 0,
      toleranceAmount: 1,
      managerApprovalRequired: false,
    });
    finalizePosSale.mockResolvedValue({
      saleId: 99,
      cashSessionId: 7,
      totalAmount: 20,
      soldAt: "2026-01-01T00:00:00Z",
      sellerId: null,
      changeAmount: 0,
    });
  });

  it("desativa o submit e mostra alerta quando a quantidade excede o saldo", async () => {
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    await pickTestProduct();
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
    await pickTestProduct();
    fireEvent.change(screen.getByLabelText(/^quantidade$/i), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText(/preço de venda unitário/i), {
      target: { value: "18" },
    });

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /registar venda/i })).not.toBeDisabled();
  });

  it("envia sellerId e pagamento CASH no finalize do PDV", async () => {
    getProductStock.mockResolvedValue({ quantity: 10 });
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    await pickTestProduct();
    fireEvent.change(screen.getByLabelText(/^quantidade$/i), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText(/preço de venda unitário/i), {
      target: { value: "18" },
    });
    fireEvent.change(await screen.findByLabelText(/^vendedora$/i), { target: { value: "11" } });
    fireEvent.click(screen.getByRole("button", { name: /registar venda/i }));
    await waitFor(() =>
      expect(finalizePosSale).toHaveBeenCalledWith(
        expect.objectContaining({
          cashSessionId: 7,
          productId: 42,
          sellerId: 11,
          payments: [{ paymentMethod: "CASH", amount: 18 }],
        }),
        expect.stringMatching(/\S/),
      ),
    );
  });

  it("envia PIX pendente quando a liquidação ainda não chegou", async () => {
    getProductStock.mockResolvedValue({ quantity: 10 });
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    await pickTestProduct();
    fireEvent.change(screen.getByLabelText(/^quantidade$/i), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText(/preço de venda unitário/i), {
      target: { value: "20" },
    });
    fireEvent.change(screen.getByLabelText(/^método de pagamento$/i), { target: { value: "PIX" } });
    fireEvent.click(screen.getByLabelText(/pendente \(aguardar liquidação real/i));
    fireEvent.click(screen.getByRole("button", { name: /registar venda/i }));
    await waitFor(() =>
      expect(finalizePosSale).toHaveBeenCalledWith(
        expect.objectContaining({
          payments: [{ paymentMethod: "PIX", amount: 20, settlementStatus: "PENDING" }],
        }),
        expect.any(String),
      ),
    );
  });

  it("envia split dinheiro e PIX no finalize", async () => {
    getProductStock.mockResolvedValue({ quantity: 10 });
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    await pickTestProduct();
    fireEvent.change(screen.getByLabelText(/^quantidade$/i), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText(/preço de venda unitário/i), {
      target: { value: "18" },
    });
    fireEvent.click(screen.getByRole("button", { name: /adicionar meio de pagamento/i }));
    const parcela1 = screen.getByRole("group", { name: /parcela 1/i });
    const parcela2 = screen.getByRole("group", { name: /parcela 2/i });
    fireEvent.change(within(parcela1).getByLabelText(/valor desta parcela/i), {
      target: { value: "10" },
    });
    fireEvent.change(within(parcela2).getByLabelText(/^método de pagamento$/i), {
      target: { value: "PIX" },
    });
    fireEvent.change(within(parcela2).getByLabelText(/valor desta parcela/i), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByRole("button", { name: /registar venda/i }));
    await waitFor(() =>
      expect(finalizePosSale).toHaveBeenCalledWith(
        expect.objectContaining({
          payments: [
            { paymentMethod: "CASH", amount: 10 },
            { paymentMethod: "PIX", amount: 8 },
          ],
        }),
        expect.any(String),
      ),
    );
  });

  it("desativa o submit enquanto o stock ainda carrega", async () => {
    getProductStock.mockImplementation(() => new Promise(() => {}));
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    fireEvent.change(screen.getByPlaceholderText(/camiseta/i), { target: { value: "c" } });
    const option = await screen.findByRole("button", { name: /#42 — Camiseta Teste/ });
    fireEvent.mouseDown(option);
    fireEvent.click(option);
    fireEvent.change(screen.getByLabelText(/^quantidade$/i), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText(/preço de venda unitário/i), {
      target: { value: "18" },
    });
    expect(screen.getByRole("button", { name: /registar venda/i })).toBeDisabled();
  });

  it("desativa o submit quando o stock falha", async () => {
    getProductStock.mockRejectedValue(new Error("stock indisponível"));
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    await pickTestProduct();
    fireEvent.change(screen.getByLabelText(/^quantidade$/i), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText(/preço de venda unitário/i), {
      target: { value: "18" },
    });
    expect(screen.getByRole("button", { name: /registar venda/i })).toBeDisabled();
  });

  it("bloqueia a venda e permite abrir caixa quando o turno está fechado", async () => {
    getCurrentCashSession.mockResolvedValue({ ...openCash, open: false, cashSessionId: null });
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    await waitFor(() => expect(screen.getByRole("button", { name: /abrir caixa/i })).toBeEnabled());
    expect(screen.getByRole("button", { name: /registar venda/i })).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/saldo inicial do caixa/i), { target: { value: "50" } });
    fireEvent.click(screen.getByRole("button", { name: /abrir caixa/i }));
    await waitFor(() => expect(openCashSession).toHaveBeenCalledWith({ openingAmount: 50 }));
  });

  it("fecha o turno com o valor contado", async () => {
    render(
      <TestQueryProvider>
        <PilotoSaleTab />
      </TestQueryProvider>,
    );
    await waitFor(() => expect(screen.getByText(/turno de caixa #7/i)).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText(/valor contado no caixa/i), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: /fechar turno/i }));
    await waitFor(() =>
      expect(closeCashSession).toHaveBeenCalledWith({
        cashSessionId: 7,
        countedAmount: 0,
        differenceReason: null,
        managerApproval: false,
      }),
    );
  });
});
