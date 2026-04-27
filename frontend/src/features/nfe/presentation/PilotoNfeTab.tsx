import { FormEvent, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  applyNfeImportSuggestions,
  importNfe,
  type NfeApplySuggestionsResponse,
  type NfeImportResponse,
} from "../../../api";
import { invalidateLojappDataQueries } from "../../../queryKeys";
import { isNfeApplySuggestionsReady } from "../domain/nfeApplyEligibility";

export function PilotoNfeTab() {
  const queryClient = useQueryClient();
  const [xml, setXml] = useState("");
  const [result, setResult] = useState<NfeImportResponse | null>(null);
  const [applyResult, setApplyResult] = useState<NfeApplySuggestionsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const importMut = useMutation({
    mutationFn: (raw: string) => importNfe(raw),
    onSuccess: async (res) => {
      setResult(res);
      setApplyResult(null);
      setXml("");
      invalidateLojappDataQueries(queryClient);
      if (res.productsCreatedWithoutSalePrice > 0) {
        toast.warning(
          `${res.productsCreatedWithoutSalePrice} produto(s) criado(s) com preço de venda igual ao custo — defina o preço de venda no catálogo.`,
        );
      }
    },
    onError: (err: unknown) => {
      setError(String(err));
    },
  });

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const raw = xml.trim();
    if (!raw) return;
    setError(null);
    setResult(null);
    setApplyResult(null);
    try {
      await importMut.mutateAsync(raw);
    } catch {
      /* estado de erro definido em onError da mutation */
    }
  }

  const busy = importMut.isPending;

  const applyMut = useMutation({
    mutationFn: (entryId: number) => applyNfeImportSuggestions(entryId),
    onSuccess: async (res) => {
      setApplyResult(res);
      invalidateLojappDataQueries(queryClient);
    },
    onError: (err: unknown) => {
      setError(String(err));
    },
  });

  const canApplySuggestions = isNfeApplySuggestionsReady(result, applyMut.isPending);

  return (
    <section className="card">
      <div className="section-head row spread" style={{ alignItems: "center" }}>
        <h2>Importar NFe (XML)</h2>
        <span className="fiscal-badge">Automação fiscal</span>
      </div>
      <p className="muted small section-lead">
        Cole o XML completo da nota fiscal. Entradas são deduplicadas pela chave de acesso (segunda
        importação: 409 Conflict).
      </p>
      <form onSubmit={onSubmit} className="form">
        <label>
          XML
          <textarea
            rows={12}
            value={xml}
            onChange={(ev) => setXml(ev.target.value)}
            placeholder='<?xml version="1.0"?> … ou trecho com &lt;prod&gt; …'
            spellCheck={false}
          />
        </label>
        {error ? <p className="error">{error}</p> : null}
        {result ? (
          <p className="success small">
            Nota <strong>{result.nfeNumber}</strong> — {result.importedItems} linha(s) — id entrada{" "}
            <strong>{result.nfeEntryId}</strong>
            {result.supplierId != null ? (
              <>
                {" "}
                — fornecedor <strong>{result.supplierId}</strong>
              </>
            ) : null}
            {result.suggestedBrandId != null ? (
              <>
                {" "}
                — marca sugerida:{" "}
                <strong>{result.suggestedBrandName ?? result.suggestedBrandId}</strong> (confirmar
                antes de aplicar)
              </>
            ) : null}
          </p>
        ) : null}
        {result && result.productsCreatedWithoutSalePrice > 0 ? (
          <p className="error small" role="status">
            <strong>Atenção:</strong> {result.productsCreatedWithoutSalePrice} produto(s) novo(s)
            com preço de venda igual ao custo de compra — atualize o preço de venda no separador
            Produtos para refletir margens corretas no dashboard.
          </p>
        ) : null}
        {result && (result.suggestedBrandId != null || result.supplierId != null) ? (
          <div className="row" style={{ gap: "0.75rem", flexWrap: "wrap", alignItems: "center" }}>
            <button
              type="button"
              className="secondary"
              disabled={!canApplySuggestions}
              onClick={() => {
                setError(null);
                void applyMut.mutateAsync(result.nfeEntryId);
              }}
            >
              {applyMut.isPending ? "A aplicar sugestões…" : "Aplicar sugestões ao catálogo"}
            </button>
            <span className="muted small">
              Preenche marca e/ou fornecedor nos produtos desta nota apenas onde ainda estão vazios.
            </span>
          </div>
        ) : null}
        {applyResult ? (
          <p className="success small">
            Sugestões aplicadas: marca em <strong>{applyResult.brandAssignedCount}</strong>{" "}
            produto(s), fornecedor em <strong>{applyResult.supplierAssignedCount}</strong>
            {applyResult.brandSkippedModelConflictCount > 0 ? (
              <>
                {" "}
                — ignoradas por modelo de catálogo:{" "}
                <strong>{applyResult.brandSkippedModelConflictCount}</strong>
              </>
            ) : null}
            .
          </p>
        ) : null}
        <button type="submit" className="primary" disabled={busy}>
          {busy ? (
            <span className="btn-inline-loading">
              <span
                className="ui-spinner"
                style={{ width: "0.95rem", height: "0.95rem", color: "#fff" }}
              />
              A processar XML…
            </span>
          ) : (
            "Importar"
          )}
        </button>
      </form>
    </section>
  );
}
