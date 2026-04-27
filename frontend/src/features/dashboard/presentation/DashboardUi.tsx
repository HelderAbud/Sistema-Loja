import { type ReactNode } from "react";

export function IconRevenue() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function IconProfit() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M3 3v18h18M7 16l4-6 4 4 4-9"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function IconTicket() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M2 9a3 3 0 0 1 0-6h20a3 3 0 0 1 0 6 3 3 0 0 1 0 6 3 3 0 0 1 0 6H2a3 3 0 0 1 0-6 3 3 0 0 1 0-6Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function IconUnits() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
      <rect x="3" y="3" width="7" height="7" rx="1" stroke="currentColor" strokeWidth="2" />
      <rect x="14" y="3" width="7" height="7" rx="1" stroke="currentColor" strokeWidth="2" />
      <rect x="3" y="14" width="7" height="7" rx="1" stroke="currentColor" strokeWidth="2" />
      <rect x="14" y="14" width="7" height="7" rx="1" stroke="currentColor" strokeWidth="2" />
    </svg>
  );
}

export function IconStock() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
      />
      <path
        d="M3.27 6.96 12 12.01l8.73-5.05M12 22.08V12"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  );
}

export function IconMargin() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M12 2 2 7l10 5 10-5-10-5ZM2 17l10 5 10-5M2 12l10 5 10-5"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function KpiTile({
  label,
  variant = "neutral",
  icon,
  sub,
  children,
  enterDelayMs = 0,
}: {
  label: string;
  variant?: "accent" | "warn" | "profit" | "neutral";
  icon: ReactNode;
  sub?: ReactNode;
  children: ReactNode;
  enterDelayMs?: number;
}) {
  const cls =
    variant === "accent"
      ? "kpi-card-accent"
      : variant === "warn"
        ? "kpi-card-warn"
        : variant === "profit"
          ? "kpi-card-profit"
          : "";
  return (
    <div
      className={`kpi-card kpi-tile kpi-enter ${cls}`.trim()}
      style={{ animationDelay: `${enterDelayMs}ms` }}
    >
      <div className="kpi-tile-head">
        <span className="kpi-icon-wrap">{icon}</span>
        <span className="kpi-label">{label}</span>
      </div>
      <strong className="kpi-value">{children}</strong>
      {sub ? <div className="kpi-sub muted small">{sub}</div> : null}
    </div>
  );
}
