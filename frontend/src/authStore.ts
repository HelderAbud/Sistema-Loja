import { create } from "zustand";

/** Chaves legadas (JWT em localStorage); removidas ao gravar sessão para não deixar tokens persistidos. */
const LEGACY_ACCESS = "lojapp_token";
const LEGACY_REFRESH = "lojapp_refresh";

function clearLegacyTokenStorage() {
  if (typeof localStorage === "undefined") return;
  localStorage.removeItem(LEGACY_ACCESS);
  localStorage.removeItem(LEGACY_REFRESH);
}

type AuthState = {
  accessToken: string | null;
  setAccessToken: (access: string | null) => void;
  /** Compatível com chamadas antigas; refresh fica em cookie HttpOnly. */
  setTokens: (access: string | null, _refreshIgnored?: string | null) => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  setAccessToken: (access) => {
    clearLegacyTokenStorage();
    set({ accessToken: access });
  },
  setTokens: (access, _refreshIgnored) => {
    clearLegacyTokenStorage();
    set({ accessToken: access });
  },
}));

export function getAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}
