import { describe, expect, it } from "vitest";
import { parseOrderPresetsImport } from "./presetImport";

describe("parseOrderPresetsImport", () => {
  it("rejeita quando presets não é array", () => {
    expect(parseOrderPresetsImport({ presets: null })).toEqual({
      ok: false,
      reason: "missing_presets",
    });
  });

  it("importa e limita a 20 entradas", () => {
    const many = Array.from({ length: 25 }, (_, i) => ({
      id: `p-${i}`,
      name: `N${i}`,
      filters: { from: "2024-01-01", to: "2024-01-02", productId: "", brandId: "" },
    }));
    const result = parseOrderPresetsImport({
      presets: many,
      defaultPresetId: "p-5",
    });
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.presets).toHaveLength(20);
    expect(result.defaultPresetId).toBe("p-5");
  });

  it("ignora defaultPresetId se não existir nos importados", () => {
    const result = parseOrderPresetsImport({
      presets: [{ id: "a", name: "A", filters: {} }],
      defaultPresetId: "missing",
    });
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.defaultPresetId).toBeNull();
  });
});
