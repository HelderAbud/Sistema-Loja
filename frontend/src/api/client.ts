import { getAccessToken, useAuthStore } from "../authStore";
import { mapUserFacingApiError } from "../shared/errors/mapUserFacingApiError";

const base = () => (import.meta.env.VITE_API_BASE ?? "").replace(/\/$/, "");

/** Corpo JSON padrão de erro da API (`GlobalExceptionHandler`). */
export type ApiErrorBody = {
  message: string;
  code: string;
  timestamp: string;
};

function url(path: string): string {
  const p = path.startsWith("/") ? path : `/${path}`;
  return `${base()}${p}`;
}

/** Resposta de login/registo/refresh: só o JWT de acesso; refresh em cookie HttpOnly. */
export type AccessTokenResponse = { accessToken: string };

/** POST /auth/refresh com cookie; não usa access atual nem apiJson (evita recursão em 401). */
async function renewAccessTokenViaRefreshCookie(): Promise<string | null> {
  try {
    const res = await fetch(url("/api/v1/auth/refresh"), {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: "{}",
      credentials: "include",
    });
    if (!res.ok) return null;
    const text = await res.text();
    if (!text) return null;
    const j = JSON.parse(text) as AccessTokenResponse;
    return j.accessToken?.trim() ? j.accessToken : null;
  } catch {
    return null;
  }
}

let bootstrapRefreshInFlight: Promise<boolean> | null = null;

/**
 * Ao arranque da SPA: tenta obter access JWT a partir da cookie HttpOnly de refresh.
 * Pedidos em paralelo (ex.: React StrictMode) partilham a mesma Promise.
 */
export function bootstrapSessionFromCookie(): Promise<boolean> {
  if (!bootstrapRefreshInFlight) {
    bootstrapRefreshInFlight = (async () => {
      const at = await renewAccessTokenViaRefreshCookie();
      if (!at) return false;
      useAuthStore.getState().setAccessToken(at);
      return true;
    })();
    void bootstrapRefreshInFlight.finally(() => {
      bootstrapRefreshInFlight = null;
    });
  }
  return bootstrapRefreshInFlight;
}

async function refreshSession(): Promise<boolean> {
  const at = await renewAccessTokenViaRefreshCookie();
  if (!at) return false;
  useAuthStore.getState().setAccessToken(at);
  return true;
}

export async function apiJson<T>(
  path: string,
  options: RequestInit = {},
  skipAuth = false,
  allowRefresh = true,
): Promise<T> {
  const token = skipAuth ? null : getAccessToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(url(path), {
    ...options,
    credentials: options.credentials ?? "include",
    headers,
  });
  if (res.status === 401 && !skipAuth && allowRefresh) {
    const renewed = await refreshSession();
    if (renewed) return apiJson<T>(path, options, skipAuth, false);
    useAuthStore.getState().setAccessToken(null);
    throw new Error("Sessão expirada ou inválida. Inicie sessão novamente.");
  }
  if (!res.ok) {
    let msg = mapUserFacingApiError(res.status, undefined, undefined);
    try {
      const text = await res.text();
      if (text) {
        try {
          const j = JSON.parse(text) as {
            message?: string;
            code?: string;
            error?: string;
          };
          const code = j.code ?? j.error;
          msg = mapUserFacingApiError(res.status, code, j.message);
        } catch {
          msg = text.length > 400 ? `${text.slice(0, 400)}…` : text;
        }
      }
    } catch {
      /* ignore */
    }
    throw new Error(msg);
  }
  if (res.status === 204) return undefined as T;
  const bodyText = await res.text();
  if (!bodyText) return undefined as T;
  return JSON.parse(bodyText) as T;
}
