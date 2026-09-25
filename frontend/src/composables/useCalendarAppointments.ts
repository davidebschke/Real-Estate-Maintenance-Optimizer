import { computed } from 'vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { isSameDay } from '@/utils/dateFormat'
import type { Appointment } from '@/types/appointment'

/** Single event object in the shape vue-cal expects, derived from an `Appointment`. */
export interface VueCalEvent {
  appointmentId: string
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
  const store = useAppointmentsStore()
  const appointments = computed(() => store.appointments)

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
  const displayEnd = appointment.completed
    ? (appointment.actualEnd ?? appointment.end)
    : appointment.end
  const classes = [`calendar-event--${appointment.category}`]
  if (appointment.completed) classes.push('calendar-event--completed')
  if (appointment.locked) classes.push('calendar-event--locked')

  return {
    appointmentId: appointment.id,
    start: toVueCalDateTime(appointment.start),
    end: toVueCalDateTime(displayEnd),
    title: appointment.title,
    content: appointment.propertyName,
    class: classes.join(' '),
  }
}

/** Formats a `Date` as the `YYYY-MM-DD HH:mm` string vue-cal expects for event start/end times. */
function toVueCalDateTime(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
