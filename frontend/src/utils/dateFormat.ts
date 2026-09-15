/** Formats a bare day-of-month number, adding the German trailing period Intl omits for a day-only format. */
export function formatLocalizedDayNumber(date: Date, locale: string): string {
  const day = new Intl.DateTimeFormat(locale, { day: 'numeric' }).format(date)
  return locale.startsWith('de') ? `${day}.` : day
}
