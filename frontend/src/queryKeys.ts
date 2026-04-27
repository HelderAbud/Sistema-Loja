import type { QueryClient } from "@tanstack/react-query";

/** Chaves estáveis para cache e invalidação (TanStack Query v5). */
export const queryKeys = {
  session: {
    me: () => ["session", "me"] as const,
  },

  brands: () => ["brands"] as const,

  products: (p: { page: number; size: number; brandId?: number; q?: string; lowStock?: boolean }) =>
    ["products", p.page, p.size, p.brandId ?? "", p.q ?? "", Boolean(p.lowStock)] as const,

  productStock: (productId: number) => ["productStock", productId] as const,

  lowStock: () => ["inventory", "lowStock"] as const,

  sales: (p: { page: number; size: number; from?: string; to?: string; productId?: number }) =>
    ["sales", p.page, p.size, p.from ?? "", p.to ?? "", p.productId ?? ""] as const,

  dashboard: {
    root: () => ["dashboard"] as const,
    brands: (rangeKey: string) => ["dashboard", "brands", rangeKey] as const,
    abc: (rangeKey: string) => ["dashboard", "abc", rangeKey] as const,
    inventory: () => ["dashboard", "inventory"] as const,
  },
};

/** Após mutações que alteram catálogo, stock, vendas ou KPIs. */
export function invalidateLojappDataQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: ["brands"] });
  void queryClient.invalidateQueries({ queryKey: ["products"] });
  void queryClient.invalidateQueries({ queryKey: ["sales"] });
  void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.root() });
  void queryClient.invalidateQueries({ queryKey: ["inventory"] });
  void queryClient.invalidateQueries({ queryKey: ["productStock"] });
}
