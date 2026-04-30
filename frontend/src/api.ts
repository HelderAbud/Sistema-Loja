/**
 * Barrel público: apenas reexporta módulos em `src/api/`. Não adicionar chamadas HTTP aqui —
 * evita regressão para um único ficheiro monolítico. Imports estáveis: `from "./api"`, `from "../api"`.
 */
export type { AccessTokenResponse, ApiErrorBody } from "./api/client";
export { apiJson, bootstrapSessionFromCookie } from "./api/client";
export * from "./api/auth";
export type { CurrentUser } from "./api/users";
export { fetchCurrentUser } from "./api/users";
export * from "./api/brands";
export * from "./api/suppliers";
export * from "./api/hierarchy";
export * from "./api/products";
export * from "./api/inventory";
export * from "./api/sales";
export * from "./api/dashboard";
export * from "./api/pos";
export type { NfeApplySuggestionsResponse, NfeImportResponse } from "./api/nfe";
export { applyNfeImportSuggestions, importNfe } from "./api/nfe";
