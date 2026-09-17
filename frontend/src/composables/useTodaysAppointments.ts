import { computed } from 'vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { isSameDay } from '@/utils/dateFormat'
import type { Appointment } from '@/types/appointment'

/** Provides today's appointments from the appointments store, sorted from the earliest to the latest start time. */
export function useTodaysAppointments() {
  const store = useAppointmentsStore()

  const todaysAppointments = computed<Appointment[]>(() => {
    const today = new Date()
    return store.appointments
      .filter((appointment) => isSameDay(appointment.start, today))
      .slice()
      .sort((a, b) => a.start.getTime() - b.start.getTime())
  })

  return { todaysAppointments }
}
