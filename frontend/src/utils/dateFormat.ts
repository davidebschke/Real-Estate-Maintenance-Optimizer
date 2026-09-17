/** Formats a bare day-of-month number, adding the German trailing period Intl omits for a day-only format. */
export function formatLocalizedDayNumber(date: Date, locale: string): string {
  const day = new Intl.DateTimeFormat(locale, { day: 'numeric' }).format(date)
  return locale.startsWith('de') ? `${day}.` : day
}

/** Returns whether two dates fall on the same calendar day. */
export function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

/** Formats a duration given in minutes as localized hours and minutes (e.g. "1 Std 30 Min" / "1 hr 30 min"), omitting the minutes part when it is zero. */
export function formatDurationMinutes(totalMinutes: number, locale: string): string {
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  const isGerman = locale.startsWith('de')
  const hoursLabel = isGerman ? 'Std' : 'hr'
  const minutesLabel = isGerman ? 'Min' : 'min'

  const parts: string[] = []
  if (hours > 0) parts.push(`${hours} ${hoursLabel}`)
  if (minutes > 0) parts.push(`${minutes} ${minutesLabel}`)

  return parts.length > 0 ? parts.join(' ') : `0 ${minutesLabel}`
}
