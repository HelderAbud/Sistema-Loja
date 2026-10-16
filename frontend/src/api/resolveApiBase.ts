/** Demo pública: front e API em sites diferentes. O cookie de refresh é SameSite=Lax. */
const DEMO_FRONT_HOST = "sistema-loja-psi.vercel.app";
const DEMO_API_HOST = "sistema-loja-production-7608.up.railway.app";

/**
 * Na demo, ignora `VITE_API_BASE` absoluto da Railway e usa o próprio host.
 * `frontend/vercel.json` faz proxy de `/api/*` para a API, e o browser grava
 * `lojapp_rt` como cookie same-origin (Safari e Chrome anônimo bloqueiam o cross-site).
 */
export function resolveApiBase(configured: string | undefined, pageHost: string): string {
  const trimmed = (configured ?? "").trim().replace(/\/$/, "");
  if (!trimmed) return "";
  let apiHost = "";
  try {
    apiHost = new URL(trimmed).host;
  } catch {
    return trimmed;
  }
  if (pageHost === DEMO_FRONT_HOST && apiHost === DEMO_API_HOST) {
    return "";
  }
  return trimmed;
}
