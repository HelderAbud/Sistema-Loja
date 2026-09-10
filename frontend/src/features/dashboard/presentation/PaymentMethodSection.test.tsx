import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PaymentMethodSection } from "./PaymentMethodSection";

describe("PaymentMethodSection", () => {
  it("mostra venda, liquidação e pendente em aberto", () => {
    render(
      <PaymentMethodSection
        payments={{
          sold: {
            confirmedTotal: 30,
            pendingTotal: 20,
            methods: [
              { paymentMethod: "CASH", confirmedAmount: 30, pendingAmount: 0 },
              { paymentMethod: "PIX", confirmedAmount: 0, pendingAmount: 20 },
            ],
          },
          settled: {
            confirmedTotal: 15,
            pendingTotal: 0,
            methods: [{ paymentMethod: "PIX", confirmedAmount: 15, pendingAmount: 0 }],
          },
          openPending: {
            confirmedTotal: 0,
            pendingTotal: 20,
            methods: [{ paymentMethod: "PIX", confirmedAmount: 0, pendingAmount: 20 }],
          },
        }}
      />,
    );
    expect(screen.getByText(/por data da venda/i)).toBeInTheDocument();
    expect(screen.getByText(/por data da liquidação/i)).toBeInTheDocument();
    expect(screen.getByText(/pendente em aberto/i)).toBeInTheDocument();
    expect(screen.getAllByText("PIX").length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText("Dinheiro")).toBeInTheDocument();
  });
});
