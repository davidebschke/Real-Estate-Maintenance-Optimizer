/** One selectable day in the appointment form's "Tag" dropdown. */
export interface DayOption {
  value: string
  label: string
}

/** One selectable time slot in the appointment form's "Uhrzeit" dropdown. */
export interface TimeOption {
  value: string
  label: string
}

/** One selectable duration in the appointment form's "Dauer" dropdown, identified by a translation key. */
export interface DurationOption {
  key: string
  minutes: number
}

/** One selectable repeat interval for a recurring appointment, identified by a translation key. */
export interface RecurrenceIntervalOption {
  key: string
  months: number
}

/** Duration choices offered for a new appointment, from a short visit up to a full day. */
export const DURATION_OPTIONS: DurationOption[] = [
  { key: 'oneHour', minutes: 60 },
  { key: 'twoHours', minutes: 120 },
  { key: 'threeHours', minutes: 180 },
  { key: 'fourHours', minutes: 240 },
  { key: 'allDay', minutes: 1440 },
]

/** Repeat interval choices offered for a recurring appointment. */
export const RECURRENCE_INTERVAL_OPTIONS: RecurrenceIntervalOption[] = [
  { key: 'monthly', months: 1 },
  { key: 'quarterly', months: 3 },
  { key: 'semiAnnually', months: 6 },
  { key: 'yearly', months: 12 },
]

/** JavaScript `Date.getDay()` value for Sunday, excluded from every selectable-day list since the company does not schedule appointments then. */
export const SUNDAY_WEEKDAY_INDEX = 0

/** Generates the next `dayCount` selectable calendar days (Sundays excluded) from `referenceDate`, formatted for the given locale (e.g. "Di., 11.08.2026"). */
export function generateUpcomingDayOptions(
  referenceDate: Date,
  locale: string,
  dayCount = 60,
): DayOption[] {
  const formatter = new Intl.DateTimeFormat(locale, {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
  const options: DayOption[] = []

  for (let dayOffset = 0; options.length < dayCount; dayOffset++) {
    const date = new Date(
      referenceDate.getFullYear(),
      referenceDate.getMonth(),
      referenceDate.getDate() + dayOffset,
    )
    if (date.getDay() === SUNDAY_WEEKDAY_INDEX) continue
    options.push({ value: toIsoDate(date), label: formatter.format(date) })
  }

  return options
}

/** Generates half-hour time slots between `startHour` and `endHour` (both inclusive of the hour boundary). */
export function generateTimeSlotOptions(
  startHour = 7,
  endHour = 19,
  stepMinutes = 30,
): TimeOption[] {
  const options: TimeOption[] = []

  for (
    let minutesFromStart = 0;
    startHour * 60 + minutesFromStart <= endHour * 60;
    minutesFromStart += stepMinutes
  ) {
    const totalMinutes = startHour * 60 + minutesFromStart
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60
    const value = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
    options.push({ value, label: value })
  }

  return options
}

/** Formats a `Date` as the `YYYY-MM-DD` day option value. */
export function toIsoDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** Formats a `Date` as the `HH:mm` time option value. */
export function toTimeString(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** Combines a day option value ("YYYY-MM-DD") and a time option value ("HH:mm") into a local `Date`. */
export function combineDayAndTime(day: string, time: string): Date {
  const [year, month, dayOfMonth] = day.split('-')
  const [hour, minute] = time.split(':')
  return new Date(
    Number(year!),
    Number(month!) - 1,
    Number(dayOfMonth!),
    Number(hour!),
    Number(minute!),
  )
}
