import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PageHeader } from "./PageHeader";

describe("PageHeader", () => {
  it("mostra título e lead", () => {
    render(<PageHeader title="Catálogo" lead="Filtre por marca." />);
    expect(screen.getByRole("heading", { name: "Catálogo" })).toBeInTheDocument();
    expect(screen.getByText("Filtre por marca.")).toBeInTheDocument();
  });

  it("omite lead quando não é passado", () => {
    render(<PageHeader title="Só título" />);
    expect(screen.getByRole("heading", { name: "Só título" })).toBeInTheDocument();
    expect(screen.queryByText(/filtre/i)).not.toBeInTheDocument();
  });
});
