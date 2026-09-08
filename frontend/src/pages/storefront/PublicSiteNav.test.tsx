import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { LandingPage } from "./LandingPage";
import { PitchPage } from "./PitchPage";
import { StoreHeader } from "./storefrontShared";

describe("área pública (nav e pitch)", () => {
  it("header só tem Home, Pitch e Entrar", () => {
    render(
      <MemoryRouter>
        <StoreHeader />
      </MemoryRouter>,
    );

    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "Pitch" })).toHaveAttribute("href", "/pitch");
    expect(screen.getByRole("link", { name: "Entrar" })).toHaveAttribute("href", "/login");
    expect(screen.queryByRole("link", { name: /catálogo/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /pedidos/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /área lojista/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /carrinho/i })).not.toBeInTheDocument();
  });

  it("landing usa chip honesto e não aponta para a vitrine", () => {
    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    );

    expect(screen.getByText(/MVP em demonstração pública/i)).toBeInTheDocument();
    expect(screen.queryByText(/\+\d+ lojas/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /explorar catálogo/i })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /entrar no painel/i })).toHaveAttribute(
      "href",
      "/login",
    );
    expect(screen.getByRole("link", { name: /ver a demo/i })).toHaveAttribute("href", "#como-usar");
    expect(screen.getByRole("heading", { name: /O que o sistema faz hoje/i })).toBeInTheDocument();
    expect(screen.getByText(/Importação de NFe por XML/i)).toBeInTheDocument();
    expect(screen.getByText(/JWT com refresh/i)).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Como testar/i })).toBeInTheDocument();
    expect(screen.getByAltText(/Ecrã de login do LojApp/i)).toBeInTheDocument();
    expect(screen.getByText("Java 21")).toBeInTheDocument();
    expect(
      screen
        .getByRole("contentinfo")
        .querySelector('a[href="https://github.com/HelderAbud/Sistema-Loja"]'),
    ).toBeTruthy();
  });

  it("pitch descreve o produto real e liga login + GitHub", () => {
    render(
      <MemoryRouter>
        <PitchPage />
      </MemoryRouter>,
    );

    expect(screen.getByText(/NFe entra por XML/i)).toBeInTheDocument();
    expect(screen.getByText(/turno de caixa/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /entrar no painel/i })).toHaveAttribute(
      "href",
      "/login",
    );
    expect(screen.getByRole("link", { name: /ver repositório/i })).toHaveAttribute(
      "href",
      "https://github.com/HelderAbud/Sistema-Loja",
    );
  });
});
