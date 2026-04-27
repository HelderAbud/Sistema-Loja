type Props = {
  title: string;
  /** Texto curto abaixo do título (opcional) */
  lead?: string;
  className?: string;
};

/** Cabeçalho de secção reutilizável (título + lead opcional). */
export function PageHeader({ title, lead, className = "" }: Props) {
  return (
    <header className={`page-header${className ? ` ${className}` : ""}`}>
      <div className="section-head">
        <h2>{title}</h2>
      </div>
      {lead ? <p className="muted small section-lead">{lead}</p> : null}
    </header>
  );
}
