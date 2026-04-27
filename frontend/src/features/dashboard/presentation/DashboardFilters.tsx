import type { FormEvent } from "react";

type Props = {
  fromDay: string;
  toDay: string;
  busy: boolean;
  setFromDay: (value: string) => void;
  setToDay: (value: string) => void;
  onSubmit: (e: FormEvent) => void | Promise<void>;
  onLoadDefault: () => void | Promise<void>;
};

export function DashboardFilters({
  fromDay,
  toDay,
  busy,
  setFromDay,
  setToDay,
  onSubmit,
  onLoadDefault,
}: Props) {
  return (
    <form onSubmit={onSubmit} className="form rowgap dashboard-filters">
      <div className="field-row">
        <label>
          De (dia)
          <input type="date" value={fromDay} onChange={(ev) => setFromDay(ev.target.value)} />
        </label>
        <label>
          Até (dia)
          <input type="date" value={toDay} onChange={(ev) => setToDay(ev.target.value)} />
        </label>
      </div>
      <div className="field-row">
        <button type="submit" className="primary" disabled={busy}>
          {busy ? (
            <span className="btn-inline-loading">
              <span
                className="ui-spinner"
                style={{ width: "0.95rem", height: "0.95rem", color: "#fff" }}
              />
              A carregar…
            </span>
          ) : (
            "Carregar intervalo"
          )}
        </button>
        <button
          type="button"
          className="ghost"
          onClick={() => void onLoadDefault()}
          disabled={busy}
        >
          Últimos 30 dias
        </button>
      </div>
    </form>
  );
}
