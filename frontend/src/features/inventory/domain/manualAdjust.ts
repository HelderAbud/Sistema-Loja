export type ManualAdjustInput = { productId: number; qty: number; reason: string };

export type ManualAdjustValidation =
  | { ok: true; value: ManualAdjustInput }
  | { ok: false; message: string };

export function validateManualStockAdjust(
  productIdRaw: string,
  quantityRaw: string,
  reasonRaw: string,
): ManualAdjustValidation {
  const pid = Number(productIdRaw);
  const qty = Number(String(quantityRaw).replace(",", "."));
  if (!Number.isFinite(pid) || pid <= 0 || !Number.isFinite(qty) || qty === 0) {
    return {
      ok: false,
      message: "Indique productId válido e quantidade ≠ 0 (positivo = entrada).",
    };
  }
  const r = reasonRaw.trim();
  if (!r) {
    return { ok: false, message: "Indique um motivo." };
  }
  return { ok: true, value: { productId: pid, qty, reason: r } };
}
