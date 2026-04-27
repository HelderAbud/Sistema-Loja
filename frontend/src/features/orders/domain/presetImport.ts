import { ORDERS_PRESET_IMPORT_MAX } from "./types";
import type { OrdersFilterPreset, SavedOrderFilters } from "./types";

export function normalizePresetName(name: string, fallbackIndex: number) {
  const trimmed = name.trim();
  return trimmed.length > 0 ? trimmed : `Preset ${fallbackIndex + 1}`;
}

export type OrderPresetsImportResult =
  | { ok: true; presets: OrdersFilterPreset[]; defaultPresetId: string | null }
  | { ok: false; reason: "missing_presets" };

function coerceFilters(raw: unknown): SavedOrderFilters {
  if (!raw || typeof raw !== "object") {
    return { from: "", to: "", productId: "", brandId: "" };
  }
  const o = raw as Record<string, unknown>;
  return {
    from: typeof o.from === "string" ? o.from : "",
    to: typeof o.to === "string" ? o.to : "",
    productId: typeof o.productId === "string" ? o.productId : "",
    brandId: typeof o.brandId === "string" ? o.brandId : "",
  };
}

/** Interpreta o conteúdo JSON já parseado (objeto raiz do ficheiro exportado). */
export function parseOrderPresetsImport(data: unknown): OrderPresetsImportResult {
  if (!data || typeof data !== "object") {
    return { ok: false, reason: "missing_presets" };
  }
  const root = data as { presets?: unknown; defaultPresetId?: unknown };
  if (!Array.isArray(root.presets)) {
    return { ok: false, reason: "missing_presets" };
  }
  const importedPresets = root.presets
    .map((preset, index) => {
      const p = preset && typeof preset === "object" ? (preset as Record<string, unknown>) : {};
      const id = typeof p.id === "string" && p.id.length > 0 ? p.id : `${Date.now()}-${index}`;
      return {
        id,
        name: normalizePresetName(typeof p.name === "string" ? p.name : "", index),
        filters: coerceFilters(p.filters),
      };
    })
    .slice(0, ORDERS_PRESET_IMPORT_MAX);

  const rawDefault = root.defaultPresetId;
  const defaultPresetId =
    typeof rawDefault === "string" && importedPresets.some((preset) => preset.id === rawDefault)
      ? rawDefault
      : null;

  return { ok: true, presets: importedPresets, defaultPresetId };
}
