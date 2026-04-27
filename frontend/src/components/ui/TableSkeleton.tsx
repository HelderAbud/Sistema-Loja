type TableSkeletonProps = {
  rows?: number;
  /** Texto para leitores de ecrã (ex.: contexto da lista). */
  label?: string;
  className?: string;
};

/**
 * Placeholder de linhas para listas/tabelas enquanto a query inicial carrega.
 * Usar com as classes globais `.table-skeleton`, `.skeleton-block`, `.skeleton-row` em `App.css`.
 */
export function TableSkeleton({
  rows = 3,
  label = "A carregar dados",
  className,
}: TableSkeletonProps) {
  return (
    <div
      className={`table-skeleton${className ? ` ${className}` : ""}`}
      role="status"
      aria-live="polite"
      aria-busy="true"
      aria-label={label}
    >
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="skeleton-block skeleton-row" aria-hidden />
      ))}
    </div>
  );
}
