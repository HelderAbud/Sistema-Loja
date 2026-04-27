import { useEffect, useMemo, useState } from "react";
import { computePreviousComparableRange, toDateInputValue } from "../domain/dateRange";
import type { OrdersFilterPreset, OrdersSortKey, SavedOrderFilters } from "../domain/types";
import {
  readSavedDefaultPresetId,
  readSavedOrderFilterPresets,
  readSavedOrderFilters,
  writeSavedDefaultPresetId,
  writeSavedOrderFilterPresets,
  writeSavedOrderFilters,
} from "../domain/storage";

export function useStorefrontOrdersFilters() {
  const savedFilters = useMemo(() => readSavedOrderFilters(), []);
  const [from, setFrom] = useState(savedFilters.from);
  const [to, setTo] = useState(savedFilters.to);
  const [productId, setProductId] = useState(savedFilters.productId);
  const [brandId, setBrandId] = useState(savedFilters.brandId);
  const [page, setPage] = useState(0);
  const [presetName, setPresetName] = useState("");
  const [filterPresets, setFilterPresets] = useState<OrdersFilterPreset[]>(() =>
    readSavedOrderFilterPresets(),
  );
  const [defaultPresetId, setDefaultPresetId] = useState<string | null>(() =>
    readSavedDefaultPresetId(),
  );
  const [editingPresetId, setEditingPresetId] = useState<string | null>(null);
  const [editingPresetName, setEditingPresetName] = useState("");
  const [ordersSortKey, setOrdersSortKey] = useState<OrdersSortKey>("soldAt");
  const [ordersSortDir, setOrdersSortDir] = useState<"asc" | "desc">("desc");

  const parsedProductId =
    productId.trim() === "" || !Number.isFinite(Number(productId)) ? undefined : Number(productId);
  const parsedBrandId =
    brandId.trim() === "" || !Number.isFinite(Number(brandId)) ? undefined : Number(brandId);

  const previousRange = useMemo(() => computePreviousComparableRange(from, to), [from, to]);

  function applyQuickRange(days: number) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    setFrom(toDateInputValue(start));
    setTo(toDateInputValue(end));
    setPage(0);
  }

  useEffect(() => {
    const payload: SavedOrderFilters = { from, to, productId, brandId };
    writeSavedOrderFilters(payload);
  }, [from, to, productId, brandId]);

  useEffect(() => {
    writeSavedOrderFilterPresets(filterPresets);
  }, [filterPresets]);

  useEffect(() => {
    writeSavedDefaultPresetId(defaultPresetId);
  }, [defaultPresetId]);

  useEffect(() => {
    if (!defaultPresetId) return;
    const preset = filterPresets.find((entry) => entry.id === defaultPresetId);
    if (!preset) {
      setDefaultPresetId(null);
      return;
    }
    setFrom(preset.filters.from);
    setTo(preset.filters.to);
    setProductId(preset.filters.productId);
    setBrandId(preset.filters.brandId);
    setPage(0);
  }, [defaultPresetId, filterPresets]);

  return {
    from,
    setFrom,
    to,
    setTo,
    productId,
    setProductId,
    brandId,
    setBrandId,
    page,
    setPage,
    presetName,
    setPresetName,
    filterPresets,
    setFilterPresets,
    defaultPresetId,
    setDefaultPresetId,
    editingPresetId,
    setEditingPresetId,
    editingPresetName,
    setEditingPresetName,
    ordersSortKey,
    setOrdersSortKey,
    ordersSortDir,
    setOrdersSortDir,
    parsedProductId,
    parsedBrandId,
    previousRange,
    applyQuickRange,
  };
}
