/** Converte dia local (`YYYY-MM-DD`) para ISO início do dia (UTC via `T00:00:00` local). */
export function toIsoStartOfDay(localDate: string): string | undefined {
  if (!localDate) return undefined;
  const d = new Date(`${localDate}T00:00:00`);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}

export function toIsoEndOfDay(localDate: string): string | undefined {
  if (!localDate) return undefined;
  const d = new Date(`${localDate}T23:59:59.999`);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}
