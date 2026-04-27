import { apiJson, type AccessTokenResponse } from "./client";

export async function login(email: string, password: string): Promise<AccessTokenResponse> {
  return apiJson<AccessTokenResponse>(
    "/api/v1/auth/login",
    {
      method: "POST",
      body: JSON.stringify({ email, password }),
    },
    true,
  );
}

export async function register(
  email: string,
  password: string,
  inviteToken?: string | null,
): Promise<AccessTokenResponse> {
  const body: Record<string, string> = { email, password };
  if (inviteToken != null && inviteToken !== "") {
    body.inviteToken = inviteToken;
  }
  return apiJson<AccessTokenResponse>(
    "/api/v1/auth/register",
    {
      method: "POST",
      body: JSON.stringify(body),
    },
    true,
  );
}

/** Limpa a cookie HttpOnly de refresh no servidor; descarte o access token no cliente. */
export async function authLogout(): Promise<void> {
  await apiJson<void>(
    "/api/v1/auth/logout",
    {
      method: "POST",
    },
    true,
  );
}
