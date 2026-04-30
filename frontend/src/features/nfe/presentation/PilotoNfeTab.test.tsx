import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { TestQueryProvider } from "@/test/queryWrapper";
import { PilotoNfeTab } from "./PilotoNfeTab";

const { importNfeMock, applyNfeImportSuggestionsMock } = vi.hoisted(() => ({
  importNfeMock: vi.fn(),
  applyNfeImportSuggestionsMock: vi.fn(),
}));

vi.mock("@/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api")>();
  return {
    ...actual,
    importNfe: importNfeMock,
    applyNfeImportSuggestions: applyNfeImportSuggestionsMock,
  };
});

describe("PilotoNfeTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    importNfeMock.mockResolvedValue({
      nfeEntryId: 1,
      nfeNumber: "NF-1",
      importedItems: 2,
      supplierId: null,
      suggestedBrandId: null,
      suggestedBrandName: null,
    });
    applyNfeImportSuggestionsMock.mockResolvedValue({
      nfeLineCount: 2,
      brandAssignedCount: 1,
      supplierAssignedCount: 0,
      brandSkippedModelConflictCount: 0,
      appliedBrandId: 9,
      appliedBrandName: "X",
      supplierIdFromEntry: null,
    });
  });

  it("submete XML e mostra resultado após importação bem-sucedida", async () => {
    render(
      <TestQueryProvider>
        <PilotoNfeTab />
      </TestQueryProvider>,
    );

    fireEvent.change(screen.getByLabelText(/xml/i), {
      target: { value: "<nfe><nNF>99</nNF></nfe>" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^importar$/i }));

    await waitFor(() => expect(importNfeMock).toHaveBeenCalledWith("<nfe><nNF>99</nNF></nfe>"));
    await waitFor(() => {
      expect(screen.getByText(/NF-1/)).toBeInTheDocument();
      expect(screen.getByText(/2 linha/)).toBeInTheDocument();
    });
  });

  it("importação com sugestão mostra botão aplicar e chama a API", async () => {
    importNfeMock.mockResolvedValue({
      nfeEntryId: 42,
      nfeNumber: "NF-42",
      importedItems: 1,
      supplierId: 7,
      suggestedBrandId: 9,
      suggestedBrandName: "Marca",
    });

    render(
      <TestQueryProvider>
        <PilotoNfeTab />
      </TestQueryProvider>,
    );

    fireEvent.change(screen.getByLabelText(/xml/i), {
      target: { value: "<nfe/>" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^importar$/i }));

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /aplicar sugestões/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("button", { name: /aplicar sugestões/i }));

    await waitFor(() => {
      expect(applyNfeImportSuggestionsMock).toHaveBeenCalledWith(42);
    });
    await waitFor(() => {
      expect(screen.getByText(/Sugestões aplicadas/)).toBeInTheDocument();
    });
  });

  it("mostra erro quando a API falha", async () => {
    importNfeMock.mockRejectedValue(new Error("409: chave duplicada"));
    render(
      <TestQueryProvider>
        <PilotoNfeTab />
      </TestQueryProvider>,
    );

    fireEvent.change(screen.getByLabelText(/xml/i), {
      target: { value: "<nfe/>" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^importar$/i }));

    await waitFor(() => {
      expect(screen.getByText(/409: chave duplicada/)).toBeInTheDocument();
    });
  });
});
