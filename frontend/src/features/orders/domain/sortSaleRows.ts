import type { OrdersSortKey } from "./types";

export type SaleRowSortable = {
  quantity: number | string;
  unitPrice: number | string;
  soldAt: string;
};

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
      const aTotal = Number(a.quantity) * Number(a.unitPrice);
      const bTotal = Number(b.quantity) * Number(b.unitPrice);
      return ordersSortDir === "asc" ? aTotal - bTotal : bTotal - aTotal;
    }
    const aTime = new Date(a.soldAt).getTime();
    const bTime = new Date(b.soldAt).getTime();
    return ordersSortDir === "asc" ? aTime - bTime : bTime - aTime;
  });
  return base;
}
