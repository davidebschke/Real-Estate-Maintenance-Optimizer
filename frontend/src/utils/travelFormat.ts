import { formatDurationMinutes } from '@/utils/dateFormat'

/** Formats a distance given in meters as localized kilometers with at most one decimal place (e.g. "6,4" / "6.4"). */
export function formatKilometers(distanceMeters: number, locale: string): string {
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(distanceMeters / 1000)
}

/** Formats a driving time given in seconds as localized hours and minutes, rounded up to the next full minute. */
export function formatTravelDuration(durationSeconds: number, locale: string): string {
  return formatDurationMinutes(Math.ceil(durationSeconds / 60), locale)
}
