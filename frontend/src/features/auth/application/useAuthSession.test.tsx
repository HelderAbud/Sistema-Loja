import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/authStore";
import { useAuthSession } from "./useAuthSession";

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    login: vi.fn(),
    register: vi.fn(),
    authLogout: vi.fn(),
    bootstrapSessionFromCookie: vi.fn(),
  },
}));

vi.mock("@/api", () => ({
  login: apiMock.login,
  register: apiMock.register,
  authLogout: apiMock.authLogout,
  bootstrapSessionFromCookie: apiMock.bootstrapSessionFromCookie,
}));

describe("useAuthSession", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().setTokens(null, null);
    apiMock.bootstrapSessionFromCookie.mockResolvedValue(false);
    apiMock.authLogout.mockResolvedValue(undefined);
  });

  it("login guarda access token no estado", async () => {
    apiMock.login.mockResolvedValue({ accessToken: "jwt-token-login" });
    const { result } = renderHook(() => useAuthSession());

    await act(async () => {
      await result.current.login("qa@lojapp.test", "senha1234");
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(useAuthStore.getState().accessToken).toBe("jwt-token-login");
  });

  it("register guarda access token no estado", async () => {
    apiMock.register.mockResolvedValue({ accessToken: "jwt-token-register" });
    const { result } = renderHook(() => useAuthSession());

    await act(async () => {
      await result.current.register("novo@lojapp.test", "senha1234");
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(useAuthStore.getState().accessToken).toBe("jwt-token-register");
  });

  it("logout limpa token mesmo com falha na API", async () => {
    useAuthStore.getState().setTokens("jwt-token-existing", null);
    apiMock.authLogout.mockRejectedValue(new Error("api indisponível"));
    const { result } = renderHook(() => useAuthSession());

    await act(async () => {
      await expect(result.current.logout()).rejects.toThrow("api indisponível");
    });
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });
});
