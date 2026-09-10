import type { SalesPaymentsSlice, SalesPaymentsSummary } from "@/api";
import { POS_PAYMENT_METHOD_OPTIONS } from "@/features/sales/domain/posPayment";
import { money } from "../domain/chartFormat";

type Props = {
  payments?: SalesPaymentsSummary;
};

function methodLabel(value: string): string {
  return POS_PAYMENT_METHOD_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

function SliceTable({
  title,
  hint,
  slice,
  showPending,
}: {
  title: string;
  hint: string;
  slice: SalesPaymentsSlice;
  showPending: boolean;
}) {
  return (
    <div className="table-wrap">
      <h3 className="chart-title">{title}</h3>
      <p className="muted small" style={{ marginBottom: "0.5rem" }}>
        {hint} Liquidado: <strong>{money(slice.confirmedTotal)}</strong>
        {showPending ? (
          <>
            {" "}
            · Pendente: <strong>{money(slice.pendingTotal)}</strong>
          </>
        ) : null}
      </p>
      {slice.methods.length === 0 ? (
        <p className="muted small">Sem movimentos nesta vista.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Método</th>
              <th>Liquidado</th>
              {showPending ? <th>Pendente</th> : null}
            </tr>
          </thead>
          <tbody>
            {slice.methods.map((row) => (
              <tr key={row.paymentMethod}>
                <td>{methodLabel(row.paymentMethod)}</td>
                <td>{money(row.confirmedAmount)}</td>
                {showPending ? <td>{money(row.pendingAmount)}</td> : null}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export function PaymentMethodSection({ payments }: Props) {
  if (!payments) return null;
  return (
    <>
      <SliceTable
        title="Pagamentos por data da venda"
        hint="O que foi vendido no período, ainda que a liquidação seja noutro dia."
        slice={payments.sold}
        showPending
      />
      <SliceTable
        title="Pagamentos por data da liquidação"
        hint="O que foi liquidado neste período, mesmo que a venda seja de outro dia."
        slice={payments.settled}
        showPending={false}
      />
      <SliceTable
        title="Pendente em aberto"
        hint="Ainda sem liquidação, independentemente da data da venda."
        slice={payments.openPending}
        showPending
      />
    </>
  );
}
