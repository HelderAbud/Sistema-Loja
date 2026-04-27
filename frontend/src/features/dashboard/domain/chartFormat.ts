export const money = (n: number) =>
  n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export const ABC_COLORS: Record<string, string> = {
  A: "#16a34a",
  B: "#ca8a04",
  C: "#64748b",
};

export const CHART_TOOLTIP = {
  contentStyle: {
    borderRadius: 10,
    border: "1px solid var(--border-strong)",
    boxShadow: "0 12px 40px rgb(15 23 42 / 12%)",
  },
} as const;
