import type { OrdersFilterPreset, SavedOrderFilters } from "./types";
import {
  ORDERS_DEFAULT_PRESET_ID_STORAGE_KEY,
  ORDERS_FILTERS_STORAGE_KEY,
  ORDERS_FILTER_PRESETS_STORAGE_KEY,
} from "./types";

export function readSavedOrderFilters(): SavedOrderFilters {
  if (typeof localStorage === "undefined") {
    return { from: "", to: "", productId: "", brandId: "" };
  }
  const raw = localStorage.getItem(ORDERS_FILTERS_STORAGE_KEY);
  if (!raw) return { from: "", to: "", productId: "", brandId: "" };
  try {
    const parsed = JSON.parse(raw) as Partial<SavedOrderFilters>;
    return {
      from: parsed.from ?? "",
      to: parsed.to ?? "",
      productId: parsed.productId ?? "",
      brandId: parsed.brandId ?? "",
    };
  } catch {
    return { from: "", to: "", productId: "", brandId: "" };
  }
}

export function writeSavedOrderFilters(filters: SavedOrderFilters) {
  if (typeof localStorage === "undefined") return;
  localStorage.setItem(ORDERS_FILTERS_STORAGE_KEY, JSON.stringify(filters));
}

export function readSavedOrderFilterPresets(): OrdersFilterPreset[] {
  if (typeof localStorage === "undefined") return [];
  const raw = localStorage.getItem(ORDERS_FILTER_PRESETS_STORAGE_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as OrdersFilterPreset[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function writeSavedOrderFilterPresets(presets: OrdersFilterPreset[]) {
  if (typeof localStorage === "undefined") return;
  localStorage.setItem(ORDERS_FILTER_PRESETS_STORAGE_KEY, JSON.stringify(presets));
}

export function readSavedDefaultPresetId(): string | null {
  if (typeof localStorage === "undefined") return null;
  return localStorage.getItem(ORDERS_DEFAULT_PRESET_ID_STORAGE_KEY);
}

export function writeSavedDefaultPresetId(id: string | null) {
  if (typeof localStorage === "undefined") return;
  if (!id) {
    localStorage.removeItem(ORDERS_DEFAULT_PRESET_ID_STORAGE_KEY);
    return;
  }
  localStorage.setItem(ORDERS_DEFAULT_PRESET_ID_STORAGE_KEY, id);
}
