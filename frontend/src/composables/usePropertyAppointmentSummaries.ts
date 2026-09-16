import { computed } from 'vue'
import { useAppointmentsStore } from '@/stores/appointments'
import type { Appointment } from '@/types/appointment'

/** Open/completed appointment counts and the next upcoming appointment for a single property. */
export interface PropertyAppointmentSummary {
  openCount: number
  completedCount: number
  nextAppointment: Appointment | null
}

/** Provides reusable, per-property appointment summaries derived from the appointments store. */
export function usePropertyAppointmentSummaries() {
  const store = useAppointmentsStore()
  const appointments = computed(() => store.appointments)

  /** Returns the open/completed counts and the next upcoming (not yet started) appointment for the given property. */
  function getSummaryFor(propertyId: string): PropertyAppointmentSummary {
    const propertyAppointments = appointments.value.filter(
      (appointment) => appointment.propertyId === propertyId,
    )
    const openAppointments = propertyAppointments.filter((appointment) => !appointment.completed)
    const completedCount = propertyAppointments.length - openAppointments.length
    const now = new Date()
    const upcomingAppointments = openAppointments
      .filter((appointment) => appointment.start.getTime() >= now.getTime())
      .sort((a, b) => a.start.getTime() - b.start.getTime())

    return {
      openCount: openAppointments.length,
      completedCount,
      nextAppointment: upcomingAppointments[0] ?? null,
    }
  }

  return { getSummaryFor }
}
