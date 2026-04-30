import { useCallback } from "react";
import {
  authLogout,
  bootstrapSessionFromCookie,
  login as loginRequest,
  register as registerRequest,
} from "@/api";
import { useAuthStore } from "@/authStore";

export function useAuthSession() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const setTokens = useAuthStore((s) => s.setTokens);

  const isAuthenticated = !!accessToken;

  const bootstrapSession = useCallback(async (): Promise<boolean> => {
    return bootstrapSessionFromCookie();
  }, []);

  const login = useCallback(
    async (email: string, password: string): Promise<void> => {
      const { accessToken: token } = await loginRequest(email, password);
      setTokens(token, null);
    },
    [setTokens],
  );

  const register = useCallback(
    async (email: string, password: string, inviteToken?: string | null): Promise<void> => {
      const { accessToken: token } = await registerRequest(email, password, inviteToken);
      setTokens(token, null);
    },
    [setTokens],
  );

  const logout = useCallback(async (): Promise<void> => {
    try {
      await authLogout();
    } finally {
      setTokens(null, null);
    }
  }, [setTokens]);

  return {
    accessToken,
    isAuthenticated,
    bootstrapSession,
    login,
    register,
    logout,
  };
}
