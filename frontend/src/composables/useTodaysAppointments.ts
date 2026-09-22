import { computed, getCurrentInstance, onMounted, onUnmounted, ref } from 'vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { isSameDay } from '@/utils/dateFormat'
import type { Appointment } from '@/types/appointment'

/** How often the current day is re-checked, so a midnight day change is picked up without a page reload. */
const DAY_CHANGE_CHECK_INTERVAL_MS = 60_000

/** Filters appointments to those scheduled on the given calendar day, sorted from the earliest to the latest start time. */
function appointmentsOnDay(appointments: Appointment[], day: Date): Appointment[] {
  return appointments
    .filter((appointment) => isSameDay(appointment.start, day))
    .slice()
    .sort((a, b) => a.start.getTime() - b.start.getTime())
}

/**
 * Provides today's and tomorrow's appointments from the appointments store, plus whether every one of
 * today's appointments is already completed; re-checks the current day periodically so a midnight day
 * change is reflected automatically.
 */
export function useTodaysAppointments() {
  const store = useAppointmentsStore()
  const now = ref(new Date())

  if (getCurrentInstance()) {
    let intervalId: ReturnType<typeof setInterval>
    onMounted(() => {
      intervalId = setInterval(() => {
        now.value = new Date()
      }, DAY_CHANGE_CHECK_INTERVAL_MS)
    })
    onUnmounted(() => clearInterval(intervalId))
  }

  const today = computed(() => now.value)
  const tomorrow = computed(
    () => new Date(now.value.getFullYear(), now.value.getMonth(), now.value.getDate() + 1),
  )

  const todaysAppointments = computed<Appointment[]>(() =>
    appointmentsOnDay(store.appointments, today.value),
  )
  const tomorrowsAppointments = computed<Appointment[]>(() =>
    appointmentsOnDay(store.appointments, tomorrow.value),
  )

  const allTodaysAppointmentsCompleted = computed(
    () =>
      todaysAppointments.value.length > 0 &&
      todaysAppointments.value.every((appointment) => appointment.completed),
  )

  return { today, tomorrow, todaysAppointments, tomorrowsAppointments, allTodaysAppointmentsCompleted }
}
