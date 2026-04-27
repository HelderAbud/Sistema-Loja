import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LoginPage } from "./LoginPage";

describe("LoginPage", () => {
  it("mostra estado busy no submit e desativa botão", () => {
    render(
      <LoginPage
        email="qa@lojapp.test"
        password="senha1234"
        authMode="login"
        error={null}
        busy={true}
        onEmailChange={vi.fn()}
        onPasswordChange={vi.fn()}
        onAuthModeChange={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    const submit = screen.getByRole("button", { name: /entrar na conta/i });
    expect(submit).toBeDisabled();
    expect(submit).toHaveTextContent(/A processar/i);
  });

  it("mostra erro e permite trocar modo para registo", () => {
    const onAuthModeChange = vi.fn();
    render(
      <LoginPage
        email=""
        password=""
        authMode="login"
        error="Email ou palavra-passe incorretos."
        busy={false}
        onEmailChange={vi.fn()}
        onPasswordChange={vi.fn()}
        onAuthModeChange={onAuthModeChange}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByText(/palavra-passe incorretos/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Criar conta" }));
    expect(onAuthModeChange).toHaveBeenCalledWith("register");
  });
});
