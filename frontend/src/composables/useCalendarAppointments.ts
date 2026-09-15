import { computed, ref } from 'vue'
import type { Appointment } from '@/types/appointment'

/** Single event object in the shape vue-cal expects, derived from an `Appointment`. */
export interface VueCalEvent {
  start: string
  end: string
  title: string
  content: string
  class: string
}

/** Number of appointments and total travel distance scheduled for a single calendar day. */
export interface DayAppointmentSummary {
  appointmentCount: number
  travelDistanceKm: number
}

/** Provides reusable access to the appointments shown on the calendar, in vue-cal's event format and as per-day summaries. */
export function useCalendarAppointments() {
  const appointments = ref<Appointment[]>([])

  const events = computed<VueCalEvent[]>(() => appointments.value.map(toVueCalEvent))

  /** Returns the appointment count and total travel distance for the given calendar day, or null when none are scheduled. */
  function getDaySummary(date: Date): DayAppointmentSummary | null {
    const dayAppointments = appointments.value.filter((appointment) =>
      isSameDay(appointment.start, date),
    )
    if (dayAppointments.length === 0) return null

    return {
      appointmentCount: dayAppointments.length,
      travelDistanceKm: dayAppointments.reduce(
        (total, appointment) => total + appointment.travelDistanceKm,
        0,
      ),
    }
  }

  return { appointments, events, getDaySummary }
}

/** Converts an `Appointment` into the event object vue-cal renders on its grid. */
function toVueCalEvent(appointment: Appointment): VueCalEvent {
  return {
    start: toVueCalDateTime(appointment.start),
    end: toVueCalDateTime(appointment.end),
    title: appointment.title,
    content: appointment.propertyName,
    class: `calendar-event--${appointment.category}`,
  }
}

/** Formats a `Date` as the `YYYY-MM-DD HH:mm` string vue-cal expects for event start/end times. */
function toVueCalDateTime(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** Returns whether two dates fall on the same calendar day. */
function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}
