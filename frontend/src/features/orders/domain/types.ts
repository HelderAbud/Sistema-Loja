export type OrdersSortKey = "soldAt" | "total" | "quantity";

export type SavedOrderFilters = {
  from: string;
  to: string;
  productId: string;
  brandId: string;
};

export type OrdersFilterPreset = {
  id: string;
  name: string;
  filters: SavedOrderFilters;
};

export const ORDERS_FILTERS_STORAGE_KEY = "lojapp_orders_filters";
export const ORDERS_FILTER_PRESETS_STORAGE_KEY = "lojapp_orders_filter_presets";
export const ORDERS_DEFAULT_PRESET_ID_STORAGE_KEY = "lojapp_orders_default_preset_id";

export const ORDERS_PRESET_IMPORT_MAX = 20;
