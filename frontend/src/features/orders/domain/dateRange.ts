export function toDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

/** Interpreta `YYYY-MM-DD` como data de calendário local (igual ao input type=date). */
export function parseDateInput(value: string) {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const [y, m, d] = value.split("-").map(Number);
  const parsed = new Date(y, m - 1, d);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

export type PreviousComparableRange = { from: string; to: string };

/** Período imediatamente anterior com a mesma duração (para comparação de KPIs). */
export function computePreviousComparableRange(
  from: string,
  to: string,
): PreviousComparableRange | null {
  const parsedFrom = parseDateInput(from);
  const parsedTo = parseDateInput(to);
  if (!parsedFrom || !parsedTo || parsedFrom > parsedTo) return null;
  const diffMs = parsedTo.getTime() - parsedFrom.getTime();
  const previousTo = new Date(parsedFrom.getTime() - 24 * 60 * 60 * 1000);
  const previousFrom = new Date(previousTo.getTime() - diffMs);
  return {
    from: toDateInputValue(previousFrom),
    to: toDateInputValue(previousTo),
  };
}
