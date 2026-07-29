import { type ChangeEvent, useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { listBrands, listSales, summarizeSales, summarizeSalesDaily } from "../../api";
import { TableSkeleton } from "../../components/ui/TableSkeleton";
import {
  type OrdersFilterPreset,
  type OrdersSortKey,
  parseOrderPresetsImport,
  sortSaleRows,
  useStorefrontOrdersFilters,
} from "../../features/orders";
import {
  csvEscape,
  formatCurrency,
  formatPercent,
  getSavedCustomSummaryText,
  getSavedSummaryTemplate,
  percentDelta,
  StoreHeader,
  SUMMARY_CUSTOM_TEXT_STORAGE_KEY,
  SUMMARY_TEMPLATE_STORAGE_KEY,
  type SummaryTemplate,
} from "./storefrontShared";

export function OrdersPage() {
  const {
    from,
    setFrom,
    to,
    setTo,
    productId,
    setProductId,
    brandId,
    setBrandId,
    page,
    setPage,
    presetName,
    setPresetName,
    filterPresets,
    setFilterPresets,
    defaultPresetId,
    setDefaultPresetId,
    editingPresetId,
    setEditingPresetId,
    editingPresetName,
    setEditingPresetName,
    ordersSortKey,
    setOrdersSortKey,
    ordersSortDir,
    setOrdersSortDir,
    parsedProductId,
    parsedBrandId,
    previousRange,
    applyQuickRange,
  } = useStorefrontOrdersFilters();

  const salesQ = useQuery({
    queryKey: [
      "storefront",
      "orders",
      { page, from, to, productId: parsedProductId, brandId: parsedBrandId },
    ],
    queryFn: () =>
      listSales({
        page,
        size: 25,
        from: from || undefined,
        to: to || undefined,
        productId: parsedProductId,
        brandId: parsedBrandId,
      }),
  });
  const brandsQ = useQuery({
    queryKey: ["storefront", "brands"],
    queryFn: listBrands,
  });
  const summaryQ = useQuery({
    queryKey: [
      "storefront",
      "orders-summary",
      { from, to, productId: parsedProductId, brandId: parsedBrandId },
    ],
    queryFn: () =>
      summarizeSales({
        from: from || undefined,
        to: to || undefined,
        productId: parsedProductId,
        brandId: parsedBrandId,
      }),
  });
  const dailyQ = useQuery({
    queryKey: [
      "storefront",
      "orders-daily",
      { from, to, productId: parsedProductId, brandId: parsedBrandId },
    ],
    queryFn: () =>
      summarizeSalesDaily({
        from: from || undefined,
        to: to || undefined,
        productId: parsedProductId,
        brandId: parsedBrandId,
      }),
  });
  const previousSummaryQ = useQuery({
    queryKey: [
      "storefront",
      "orders-summary-previous",
      {
        from: previousRange?.from,
        to: previousRange?.to,
        productId: parsedProductId,
        brandId: parsedBrandId,
      },
    ],
    queryFn: () =>
      summarizeSales({
        from: previousRange?.from,
        to: previousRange?.to,
        productId: parsedProductId,
        brandId: parsedBrandId,
      }),
    enabled: previousRange != null,
  });

  const rows = useMemo(
    () => sortSaleRows(salesQ.data?.content ?? [], ordersSortKey, ordersSortDir),
    [salesQ.data, ordersSortKey, ordersSortDir],
  );
  const summaryPreview = buildSummaryText();
  const [customSummaryText, setCustomSummaryText] = useState(() => getSavedCustomSummaryText());
  const [isSummaryDirty, setIsSummaryDirty] = useState(false);
  const [summaryTemplate, setSummaryTemplate] = useState<SummaryTemplate>(() =>
    getSavedSummaryTemplate(),
  );

  useEffect(() => {
    if (!summaryPreview) {
      setCustomSummaryText("");
      setIsSummaryDirty(false);
      return;
    }
    if (!isSummaryDirty) {
      setCustomSummaryText(summaryPreview);
    }
  }, [summaryPreview, isSummaryDirty]);

  useEffect(() => {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(SUMMARY_TEMPLATE_STORAGE_KEY, summaryTemplate);
  }, [summaryTemplate]);

  useEffect(() => {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(SUMMARY_CUSTOM_TEXT_STORAGE_KEY, customSummaryText);
  }, [customSummaryText]);

  function exportCsv() {
    const header = ["pedido", "produto", "marca", "quantidade", "total", "data"];
    const lines = rows.map((row) =>
      [
        row.id,
        row.productName,
        row.brandName,
        row.quantity,
        Number(row.unitPrice) * Number(row.quantity),
        new Date(row.soldAt).toISOString(),
      ]
        .map(csvEscape)
        .join(","),
    );
    const summaryLines =
      summaryQ.data && previousSummaryQ.data
        ? [
            "",
            "resumo,valor",
            `faturamento_atual,${csvEscape(summaryQ.data.revenue)}`,
            `unidades_atuais,${csvEscape(summaryQ.data.unitsSold)}`,
            `ticket_medio_atual,${csvEscape(summaryQ.data.averageTicket)}`,
            `variacao_faturamento_percentual,${csvEscape(percentDelta(summaryQ.data.revenue, previousSummaryQ.data.revenue).toFixed(2))}`,
            `variacao_unidades_percentual,${csvEscape(percentDelta(summaryQ.data.unitsSold, previousSummaryQ.data.unitsSold).toFixed(2))}`,
            `variacao_ticket_medio_percentual,${csvEscape(percentDelta(summaryQ.data.averageTicket, previousSummaryQ.data.averageTicket).toFixed(2))}`,
          ]
        : [];

    const csv = [header.join(","), ...lines, ...summaryLines].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `lojapp-pedidos-${from || "inicio"}-${to || "agora"}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }

  function exportDailyCsv() {
    const header = ["data", "faturamento", "unidades_vendidas"];
    const lines = (dailyQ.data ?? []).map((point) =>
      [point.date, Number(point.revenue), Number(point.unitsSold)].map(csvEscape).join(","),
    );
    const csv = [header.join(","), ...lines].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `lojapp-evolucao-diaria-${from || "inicio"}-${to || "agora"}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }

  async function copySummary() {
    const summary = customSummaryText.trim() || summaryPreview;
    if (!summary) return;

    try {
      await navigator.clipboard.writeText(summary);
      toast.success("Resumo copiado para a área de transferência.");
    } catch {
      toast.error("Não foi possível copiar o resumo.");
    }
  }

  function buildSummaryText(template: SummaryTemplate = summaryTemplate) {
    if (!summaryQ.data) return null;
    const periodLabel = `${from || "início"} até ${to || "agora"}`;
    const revenueText = formatCurrency(summaryQ.data.revenue);
    const unitsText = Number(summaryQ.data.unitsSold).toLocaleString("pt-BR");
    const ticketText = formatCurrency(summaryQ.data.averageTicket);
    const varianceTextRaw =
      previousSummaryQ.data && previousRange
        ? `\nVariação vs período anterior (${previousRange.from} a ${previousRange.to}):\n- Faturamento: ${formatPercent(percentDelta(summaryQ.data.revenue, previousSummaryQ.data.revenue))}\n- Unidades: ${formatPercent(percentDelta(summaryQ.data.unitsSold, previousSummaryQ.data.unitsSold))}\n- Ticket médio: ${formatPercent(percentDelta(summaryQ.data.averageTicket, previousSummaryQ.data.averageTicket))}`
        : "";
    const varianceInline =
      previousSummaryQ.data && previousRange
        ? ` | Var. fat.: ${formatPercent(percentDelta(summaryQ.data.revenue, previousSummaryQ.data.revenue))}`
        : "";

    if (template === "executive") {
      return `Resumo Executivo - LojApp\nPeríodo analisado: ${periodLabel}\nIndicadores principais:\n- Faturamento consolidado: ${revenueText}\n- Unidades vendidas: ${unitsText}\n- Ticket médio: ${ticketText}${varianceTextRaw}\nRecomendação: manter acompanhamento diário e atuar em produtos com maior tração.`;
    }
    if (template === "whatsapp") {
      return `LojApp | ${periodLabel}\nFaturamento: ${revenueText}\nUnidades: ${unitsText}\nTicket: ${ticketText}${varianceInline}`;
    }
    return `Resumo comercial LojApp\nPeríodo: ${periodLabel}\n- Faturamento: ${revenueText}\n- Unidades vendidas: ${unitsText}\n- Ticket médio: ${ticketText}${varianceTextRaw}`;
  }

  function exportPresetsJson() {
    const payload = {
      exportedAt: new Date().toISOString(),
      presets: filterPresets,
      defaultPresetId,
    };
    const blob = new Blob([JSON.stringify(payload, null, 2)], {
      type: "application/json;charset=utf-8;",
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "lojapp-order-presets.json";
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }

  function importPresetsJson(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      try {
        const parsed = JSON.parse(String(reader.result ?? "{}"));
        const result = parseOrderPresetsImport(parsed);
        if (!result.ok) {
          toast.error("Arquivo inválido: presets não encontrados.");
          return;
        }
        setFilterPresets(result.presets);
        setDefaultPresetId(result.defaultPresetId);
        toast.success("Presets importados com sucesso.");
      } catch {
        toast.error("Não foi possível ler o JSON de presets.");
      } finally {
        event.target.value = "";
      }
    };
    reader.readAsText(file);
  }

  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-card">
          <h2>Histórico de pedidos</h2>
          <p className="store-muted">Últimas vendas registadas no backend do LojApp.</p>
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              setPage(0);
              void salesQ.refetch();
            }}
          >
            <div className="store-cta-row">
              <button type="button" className="ghost" onClick={() => applyQuickRange(7)}>
                7d
              </button>
              <button type="button" className="ghost" onClick={() => applyQuickRange(30)}>
                30d
              </button>
              <button type="button" className="ghost" onClick={() => applyQuickRange(90)}>
                90d
              </button>
            </div>
            <div className="field-row">
              <label>
                Ordenar por
                <select
                  value={ordersSortKey}
                  onChange={(event) => setOrdersSortKey(event.target.value as OrdersSortKey)}
                >
                  <option value="soldAt">Data</option>
                  <option value="total">Total</option>
                  <option value="quantity">Quantidade</option>
                </select>
              </label>
              <label>
                Direção
                <select
                  value={ordersSortDir}
                  onChange={(event) => setOrdersSortDir(event.target.value as "asc" | "desc")}
                >
                  <option value="desc">Descendente</option>
                  <option value="asc">Ascendente</option>
                </select>
              </label>
              <label>
                Template do resumo
                <select
                  value={summaryTemplate}
                  onChange={(event) => {
                    const nextTemplate = event.target.value as SummaryTemplate;
                    setSummaryTemplate(nextTemplate);
                    setCustomSummaryText(buildSummaryText(nextTemplate) ?? "");
                    setIsSummaryDirty(false);
                  }}
                >
                  <option value="direct">Direto</option>
                  <option value="executive">Executivo</option>
                  <option value="whatsapp">WhatsApp curto</option>
                </select>
              </label>
              <label>
                De
                <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
              </label>
              <label>
                Até
                <input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
              </label>
              <label>
                Product ID
                <input
                  value={productId}
                  onChange={(event) => setProductId(event.target.value)}
                  placeholder="ex.: 12"
                />
              </label>
              <label>
                Marca
                <select value={brandId} onChange={(event) => setBrandId(event.target.value)}>
                  <option value="">Todas</option>
                  {(brandsQ.data ?? []).map((brand) => (
                    <option key={brand.id} value={String(brand.id)}>
                      {brand.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Nome do preset
                <input
                  value={presetName}
                  onChange={(event) => setPresetName(event.target.value)}
                  placeholder="ex.: Minha visão semanal"
                />
              </label>
            </div>
            <div className="store-cta-row">
              <button type="submit" className="primary" disabled={salesQ.isFetching}>
                Aplicar filtros
              </button>
              <button
                type="button"
                className="ghost"
                onClick={exportCsv}
                disabled={rows.length === 0}
              >
                Exportar CSV
              </button>
              <button
                type="button"
                className="ghost"
                onClick={exportDailyCsv}
                disabled={!dailyQ.data || dailyQ.data.length === 0}
              >
                Exportar CSV diário
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  void copySummary();
                }}
                disabled={!summaryQ.data}
              >
                Copiar resumo
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  const summary = customSummaryText.trim() || summaryPreview;
                  if (!summary) return;
                  const url = `https://wa.me/?text=${encodeURIComponent(summary)}`;
                  window.open(url, "_blank", "noopener,noreferrer");
                }}
                disabled={!summaryQ.data}
              >
                Enviar para WhatsApp
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  setFrom("");
                  setTo("");
                  setProductId("");
                  setBrandId("");
                  setPage(0);
                }}
              >
                Limpar
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  const name = presetName.trim();
                  if (!name) {
                    toast.error("Informe um nome para o preset.");
                    return;
                  }
                  const nextPreset: OrdersFilterPreset = {
                    id: `${Date.now()}`,
                    name,
                    filters: { from, to, productId, brandId },
                  };
                  setFilterPresets((current) => [nextPreset, ...current].slice(0, 8));
                  setPresetName("");
                  toast.success("Preset de filtro salvo.");
                }}
              >
                Salvar preset
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  setSummaryTemplate("direct");
                  setCustomSummaryText("");
                  setIsSummaryDirty(false);
                  if (typeof localStorage !== "undefined") {
                    localStorage.removeItem(SUMMARY_TEMPLATE_STORAGE_KEY);
                    localStorage.removeItem(SUMMARY_CUSTOM_TEXT_STORAGE_KEY);
                  }
                  toast.success("Preferências de resumo limpas.");
                }}
              >
                Limpar preferências
              </button>
              <button
                type="button"
                className="ghost"
                onClick={exportPresetsJson}
                disabled={filterPresets.length === 0}
              >
                Exportar presets
              </button>
              <label className="ghost" style={{ display: "inline-flex", alignItems: "center" }}>
                Importar presets
                <input
                  type="file"
                  accept="application/json"
                  onChange={importPresetsJson}
                  style={{ display: "none" }}
                />
              </label>
            </div>
            {filterPresets.length > 0 ? (
              <div className="store-cta-row">
                {filterPresets.map((preset, index) => (
                  <div key={preset.id} className="store-cta-row">
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        setFrom(preset.filters.from);
                        setTo(preset.filters.to);
                        setProductId(preset.filters.productId);
                        setBrandId(preset.filters.brandId);
                        setPage(0);
                        toast.success(`Preset "${preset.name}" aplicado.`);
                      }}
                      title={preset.name}
                    >
                      {preset.name}
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        setEditingPresetId(preset.id);
                        setEditingPresetName(preset.name);
                      }}
                      title={`Renomear ${preset.name}`}
                    >
                      Renomear
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      disabled={index === 0}
                      onClick={() => {
                        if (index === 0) return;
                        setFilterPresets((current) => {
                          const next = [...current];
                          const previousIndex = index - 1;
                          [next[previousIndex], next[index]] = [next[index], next[previousIndex]];
                          return next;
                        });
                      }}
                      title={`Mover ${preset.name} para cima`}
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      disabled={index === filterPresets.length - 1}
                      onClick={() => {
                        if (index === filterPresets.length - 1) return;
                        setFilterPresets((current) => {
                          const next = [...current];
                          const nextIndex = index + 1;
                          [next[nextIndex], next[index]] = [next[index], next[nextIndex]];
                          return next;
                        });
                      }}
                      title={`Mover ${preset.name} para baixo`}
                    >
                      ↓
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        if (defaultPresetId === preset.id) {
                          setDefaultPresetId(null);
                          toast.success(`Preset "${preset.name}" removido como padrão.`);
                          return;
                        }
                        setDefaultPresetId(preset.id);
                        toast.success(`Preset "${preset.name}" definido como padrão.`);
                      }}
                      title={
                        defaultPresetId === preset.id
                          ? `Remover padrão de ${preset.name}`
                          : `Definir ${preset.name} como padrão`
                      }
                    >
                      {defaultPresetId === preset.id ? "Padrão ✓" : "Definir padrão"}
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        setFilterPresets((current) =>
                          current.filter((entry) => entry.id !== preset.id),
                        );
                        if (defaultPresetId === preset.id) {
                          setDefaultPresetId(null);
                        }
                        toast.success(`Preset "${preset.name}" removido.`);
                      }}
                      title={`Remover ${preset.name}`}
                    >
                      Remover
                    </button>
                  </div>
                ))}
              </div>
            ) : null}
            {editingPresetId ? (
              <div className="store-cta-row">
                <input
                  value={editingPresetName}
                  onChange={(event) => setEditingPresetName(event.target.value)}
                  placeholder="Novo nome do preset"
                />
                <button
                  type="button"
                  className="ghost"
                  onClick={() => {
                    const normalized = editingPresetName.trim();
                    if (!normalized) {
                      toast.error("Informe um nome válido para renomear.");
                      return;
                    }
                    setFilterPresets((current) =>
                      current.map((entry) =>
                        entry.id === editingPresetId ? { ...entry, name: normalized } : entry,
                      ),
                    );
                    setEditingPresetId(null);
                    setEditingPresetName("");
                    toast.success("Preset renomeado.");
                  }}
                >
                  Confirmar renomeação
                </button>
                <button
                  type="button"
                  className="ghost"
                  onClick={() => {
                    setEditingPresetId(null);
                    setEditingPresetName("");
                  }}
                >
                  Cancelar
                </button>
              </div>
            ) : null}
          </form>
          {salesQ.isPending ? (
            <TableSkeleton rows={4} label="A carregar pedidos" className="store-orders-skel" />
          ) : null}
          {salesQ.error ? <p className="error banner">{String(salesQ.error)}</p> : null}
          {summaryQ.data ? (
            <div className="store-kpis">
              <article>
                <strong>{formatCurrency(summaryQ.data.revenue)}</strong>
                <span>faturamento global do período</span>
              </article>
              <article>
                <strong>{summaryQ.data.unitsSold}</strong>
                <span>itens vendidos no período</span>
              </article>
              <article>
                <strong>{formatCurrency(summaryQ.data.averageTicket)}</strong>
                <span>ticket médio por pedido</span>
              </article>
            </div>
          ) : null}
          {summaryQ.data && previousSummaryQ.data && previousRange ? (
            <div className="store-kpis">
              <article>
                <strong>
                  {percentDelta(summaryQ.data.revenue, previousSummaryQ.data.revenue).toFixed(1)}%
                </strong>
                <span>
                  variação do faturamento vs período anterior ({previousRange.from} a{" "}
                  {previousRange.to})
                </span>
              </article>
              <article>
                <strong>
                  {percentDelta(summaryQ.data.unitsSold, previousSummaryQ.data.unitsSold).toFixed(
                    1,
                  )}
                  %
                </strong>
                <span>variação de unidades vendidas</span>
              </article>
              <article>
                <strong>
                  {percentDelta(
                    summaryQ.data.averageTicket,
                    previousSummaryQ.data.averageTicket,
                  ).toFixed(1)}
                  %
                </strong>
                <span>variação de ticket médio</span>
              </article>
            </div>
          ) : null}
          {summaryPreview ? (
            <div className="store-card" style={{ marginTop: "0.75rem" }}>
              <h3 style={{ marginTop: 0 }}>Preview do resumo</h3>
              <textarea
                value={customSummaryText}
                onChange={(event) => {
                  setCustomSummaryText(event.target.value);
                  setIsSummaryDirty(true);
                }}
                rows={9}
                style={{ marginBottom: "0.6rem" }}
              />
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  if (!summaryPreview) return;
                  setCustomSummaryText(summaryPreview);
                  setIsSummaryDirty(false);
                }}
              >
                Restaurar texto automático
              </button>
            </div>
          ) : null}
          {rows.length === 0 && !salesQ.isPending ? (
            <p className="store-muted">Ainda não existem pedidos.</p>
          ) : null}
          {rows.length > 0 ? (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Pedido</th>
                    <th>Produto</th>
                    <th>Marca</th>
                    <th>Qtd</th>
                    <th>Total</th>
                    <th>Data</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.id}>
                      <td>#{row.id}</td>
                      <td>{row.productName}</td>
                      <td>{row.brandName}</td>
                      <td>{row.quantity}</td>
                      <td>{formatCurrency(row.unitPrice * row.quantity)}</td>
                      <td>{new Date(row.soldAt).toLocaleString("pt-BR")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          {dailyQ.data && dailyQ.data.length > 0 ? (
            <div className="chart-row chart-row-single">
              <div className="chart-card chart-surface-enter">
                <h3 className="chart-title">Evolução diária de vendas</h3>
                <p className="chart-subtitle muted small">
                  Faturamento diário dentro do filtro atual.
                </p>
                <div className="chart-inner">
                  <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart
                      data={dailyQ.data.map((point) => ({
                        date: new Date(point.date).toLocaleDateString("pt-BR"),
                        revenue: Number(point.revenue),
                        unitsSold: Number(point.unitsSold),
                      }))}
                      margin={{ top: 8, right: 8, left: 0, bottom: 4 }}
                    >
                      <defs>
                        <linearGradient id="salesDailyGradient" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="var(--chart-blue)" stopOpacity={0.35} />
                          <stop offset="100%" stopColor="var(--chart-blue)" stopOpacity={0.02} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke="var(--chart-grid)"
                        vertical={false}
                      />
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 11, fill: "var(--muted)" }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        yAxisId="left"
                        tick={{ fontSize: 11, fill: "var(--muted)" }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        yAxisId="right"
                        orientation="right"
                        tick={{ fontSize: 11, fill: "var(--muted)" }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <Tooltip
                        formatter={(value, name) =>
                          name === "Unidades"
                            ? Number(value).toLocaleString("pt-BR")
                            : formatCurrency(Number(value))
                        }
                        labelStyle={{ fontWeight: 600 }}
                      />
                      <Legend wrapperStyle={{ fontSize: 12 }} />
                      <Area
                        yAxisId="left"
                        type="monotone"
                        dataKey="revenue"
                        name="Faturamento"
                        stroke="var(--chart-blue)"
                        strokeWidth={2}
                        fill="url(#salesDailyGradient)"
                      />
                      <Line
                        yAxisId="right"
                        type="monotone"
                        dataKey="unitsSold"
                        name="Unidades"
                        stroke="var(--chart-green)"
                        strokeWidth={2}
                        dot={false}
                      />
                    </ComposedChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>
          ) : null}
          {salesQ.data ? (
            <div className="pager">
              <button
                type="button"
                className="ghost"
                disabled={salesQ.data.first || salesQ.isFetching}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                Anterior
              </button>
              <button
                type="button"
                className="ghost"
                disabled={salesQ.data.last || salesQ.isFetching}
                onClick={() => setPage((current) => current + 1)}
              >
                Seguinte
              </button>
            </div>
          ) : null}
        </section>
      </main>
    </div>
  );
}
