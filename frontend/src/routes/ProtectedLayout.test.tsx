import { render, screen } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ProtectedLayout } from "./ProtectedLayout";

describe("ProtectedLayout", () => {
  it("redireciona para /login quando não autenticado", async () => {
    render(
      <MemoryRouter initialEntries={["/piloto/products"]}>
        <Routes>
          <Route element={<ProtectedLayout authed={false} />}>
            <Route path="/piloto/products" element={<div>Página Privada</div>} />
          </Route>
          <Route path="/login" element={<div>Página Login</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Página Login")).toBeInTheDocument();
  });

  it("renderiza conteúdo privado quando autenticado", async () => {
    render(
      <MemoryRouter initialEntries={["/piloto/products"]}>
        <Routes>
          <Route element={<ProtectedLayout authed={true} />}>
            <Route path="/piloto/products" element={<div>Página Privada</div>} />
          </Route>
          <Route path="/login" element={<div>Página Login</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Página Privada")).toBeInTheDocument();
  });
});
