import type { Seller } from "@/api";

type Props = {
  sellers: Seller[];
  value: string;
  onChange: (sellerId: string) => void;
  disabled?: boolean;
};

export function SellerPicker({ sellers, value, onChange, disabled }: Props) {
  const active = sellers.filter((s) => s.active);
  return (
    <label>
      Vendedora
      <select
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        aria-label="Vendedora"
      >
        <option value="">Fila automática</option>
        {active.map((seller) => (
          <option key={seller.id} value={String(seller.id)}>
            {seller.displayName}
          </option>
        ))}
      </select>
    </label>
  );
}
