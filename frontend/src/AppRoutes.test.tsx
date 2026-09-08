import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "./App";

const bootstrapSession = vi.fn().mockResolvedValue(undefined);
const logout = vi.fn();

vi.mock("@/features/auth", () => ({
  useAuthSession: () => ({
    isAuthenticated: false,
    bootstrapSession,
    logout,
  }),
  useLoginForm: () => ({
    email: "",
    password: "",
    authMode: "login" as const,
    error: null,
    busy: false,
    setEmail: vi.fn(),
    setPassword: vi.fn(),
    setAuthMode: vi.fn(),
    setError: vi.fn(),
    onSubmit: vi.fn((e: { preventDefault: () => void }) => e.preventDefault()),
  }),
}));

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes />
    </MemoryRouter>,
  );
}

describe("AppRoutes (área pública)", () => {
  beforeEach(() => {
    bootstrapSession.mockClear();
  });

  it("mostra a landing em /", async () => {
    renderAt("/");
    expect(await screen.findByRole("heading", { level: 1 })).toHaveTextContent(/planilha/i);
  });

  it.each(["/home", "/catalog", "/orders", "/cart", "/seller", "/product/tenis-urbano-lx"])(
    "redireciona %s para a landing",
    async (path) => {
      renderAt(path);
      expect(await screen.findByRole("heading", { level: 1 })).toHaveTextContent(/planilha/i);
      expect(screen.queryByText(/Catálogo em modo demo/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Home premium da loja/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Carrinho simples/i)).not.toBeInTheDocument();
    },
  );

  it("mostra o pitch em /pitch", async () => {
    renderAt("/pitch");
    expect(await screen.findByText(/Pitch técnico/i)).toBeInTheDocument();
  });

  it("mostra login em /login quando anónimo", async () => {
    renderAt("/login");
    expect(await screen.findByLabelText("Email")).toBeInTheDocument();
  });

  it("protege /piloto/* e manda anónimo para login", async () => {
    renderAt("/piloto/products");
    expect(await screen.findByLabelText("Email")).toBeInTheDocument();
  });

  it("redireciona /app anónimo para login", async () => {
    renderAt("/app");
    expect(await screen.findByLabelText("Email")).toBeInTheDocument();
  });

  it("rota desconhecida mostra 404 em vez da landing", async () => {
    renderAt("/nao-existe");
    expect(await screen.findByRole("heading", { level: 1 })).toHaveTextContent(
      /Esta página não existe/i,
    );
    expect(screen.queryByText(/planilha/i)).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /ir para a home/i })).toHaveAttribute("href", "/");
  });
});
