import type { OrdersSortKey } from "./types";

export type SaleRowSortable = {
  quantity: number | string;
  unitPrice: number | string;
  soldAt: string;
  lineTotal?: number | string | null;
};

function merchandiseTotal(row: SaleRowSortable): number {
  if (row.lineTotal != null && row.lineTotal !== "") {
    return Number(row.lineTotal);
  }
  return Number(row.quantity) * Number(row.unitPrice);
}

export function sortSaleRows<T extends SaleRowSortable>(
  rows: T[],
  ordersSortKey: OrdersSortKey,
  ordersSortDir: "asc" | "desc",
): T[] {
  const base = [...rows];
  base.sort((a, b) => {
    if (ordersSortKey === "quantity") {
      return ordersSortDir === "asc"
        ? Number(a.quantity) - Number(b.quantity)
        : Number(b.quantity) - Number(a.quantity);
    }
    if (ordersSortKey === "total") {
      const aTotal = merchandiseTotal(a);
      const bTotal = merchandiseTotal(b);
      return ordersSortDir === "asc" ? aTotal - bTotal : bTotal - aTotal;
    }
    const aTime = new Date(a.soldAt).getTime();
    const bTime = new Date(b.soldAt).getTime();
    return ordersSortDir === "asc" ? aTime - bTime : bTime - aTime;
  });
  return base;
}
