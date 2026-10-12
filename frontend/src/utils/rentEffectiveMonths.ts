import { formatMonthYear, periodKey, type PeriodOption } from '@/utils/paymentPeriods'

/** First year from which a rent change may apply, mirroring the backend. */
export const RENT_EFFECTIVE_FIRST_YEAR = 2000

/** Builds the `YYYY-MM` key of the month the given date lies in. */
export function currentMonthKey(now: Date): string {
  return periodKey(now.getFullYear(), now.getMonth() + 1)
}

/** Lists every month from the given date's month back to January of the first allowed year, newest first. */
export function listRentEffectiveMonths(now: Date, locale: string): PeriodOption[] {
  const options: PeriodOption[] = []
  for (let year = now.getFullYear(); year >= RENT_EFFECTIVE_FIRST_YEAR; year--) {
    const lastMonth = year === now.getFullYear() ? now.getMonth() + 1 : 12
    for (let month = lastMonth; month >= 1; month--) {
      options.push({ value: periodKey(year, month), label: formatMonthYear(year, month, locale) })
    }
  }
  return options
}
