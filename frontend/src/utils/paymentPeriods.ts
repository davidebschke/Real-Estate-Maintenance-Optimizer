/** A selectable month of a given year, identified by a `YYYY-MM` key. */
export interface PeriodOption {
  value: string
  label: string
}

/** How many years before and after the anchor year the generated period options reach. */
const YEARS_BEFORE = 2
const YEARS_AFTER = 1

/** Builds the `YYYY-MM` key identifying a month of a year. */
export function periodKey(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`
}

/** Splits a `YYYY-MM` key back into its year and month. */
export function parsePeriodKey(key: string): { year: number; month: number } {
  const [year, month] = key.split('-').map(Number)
  return { year: year!, month: month! }
}

/** Formats a month of a year as its localized long month name and year (e.g. "Oktober 2026"). */
export function formatMonthYear(year: number, month: number, locale: string): string {
  return new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(new Date(year, month - 1, 1))
}

/** Formats a month number (1 to 12) as its localized short month name. */
export function formatShortMonth(month: number, locale: string): string {
  return new Intl.DateTimeFormat(locale, { month: 'short' }).format(new Date(2000, month - 1, 1))
}

/** Lists every month from two years before to one year after the anchor year in chronological order, plus the given extra months if they lie outside that range. */
export function generatePeriodOptions(anchorYear: number, locale: string, extraKeys: string[] = []): PeriodOption[] {
  const keys = new Set<string>(extraKeys)
  for (let year = anchorYear - YEARS_BEFORE; year <= anchorYear + YEARS_AFTER; year++) {
    for (let month = 1; month <= 12; month++) keys.add(periodKey(year, month))
  }
  return [...keys].sort().map((key) => {
    const { year, month } = parsePeriodKey(key)
    return { value: key, label: formatMonthYear(year, month, locale) }
  })
}
