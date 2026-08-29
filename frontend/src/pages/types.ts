export type PilotoTab =
  "products" | "sales" | "brands" | "nfe" | "inventory" | "sale" | "dashboard" | "commissions";

export const PILOTO_TABS = [
  "products",
  "sales",
  "brands",
  "nfe",
  "inventory",
  "sale",
  "dashboard",
  "commissions",
] as const satisfies readonly PilotoTab[];

export function isPilotoTab(s: string | undefined): s is PilotoTab {
  return s !== undefined && (PILOTO_TABS as readonly string[]).includes(s);
}

export const DEFAULT_PILOTO_TAB: PilotoTab = "products";
