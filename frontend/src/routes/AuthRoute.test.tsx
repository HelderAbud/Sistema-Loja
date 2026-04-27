import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AuthRoute } from "./AuthRoute";

function renderAuthRoute(authed: boolean) {
  const onSubmit = vi.fn((e: React.FormEvent) => e.preventDefault());
  const onEmailChange = vi.fn();
  const onPasswordChange = vi.fn();
  const onAuthModeChange = vi.fn();

  render(
    <MemoryRouter initialEntries={["/login"]}>
      <Routes>
        <Route
          path="/login"
          element={
            <AuthRoute
              authed={authed}
              email="qa@lojapp.test"
              password="senha1234"
              authMode="login"
              error={null}
              busy={false}
              onEmailChange={onEmailChange}
              onPasswordChange={onPasswordChange}
              onAuthModeChange={onAuthModeChange}
              onSubmit={onSubmit}
            />
          }
        />
        <Route path="/piloto/products" element={<div>Painel Produtos</div>} />
      </Routes>
    </MemoryRouter>,
  );

  return { onSubmit };
}

describe("AuthRoute", () => {
  it("redireciona para painel quando autenticado", async () => {
    renderAuthRoute(true);
    expect(await screen.findByText("Painel Produtos")).toBeInTheDocument();
  });

  it("mostra login e dispara submit quando não autenticado", () => {
    const { onSubmit } = renderAuthRoute(false);
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
    const form = document.querySelector("form.form");
    if (!form) throw new Error("Formulário de autenticação não encontrado");
    fireEvent.submit(form);
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });
});
