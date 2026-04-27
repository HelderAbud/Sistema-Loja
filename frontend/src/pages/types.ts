export type PilotoTab =
  | "products"
  | "sales"
  | "brands"
  | "nfe"
  | "inventory"
  | "sale"
  | "dashboard";

export const PILOTO_TABS = [
  "products",
  "sales",
  "brands",
  "nfe",
  "inventory",
  "sale",
  "dashboard",
] as const satisfies readonly PilotoTab[];

export function isPilotoTab(s: string | undefined): s is PilotoTab {
  return s !== undefined && (PILOTO_TABS as readonly string[]).includes(s);
}

export const DEFAULT_PILOTO_TAB: PilotoTab = "products";
