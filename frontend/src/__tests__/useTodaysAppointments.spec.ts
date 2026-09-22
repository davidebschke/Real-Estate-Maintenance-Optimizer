import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useTodaysAppointments } from '@/composables/useTodaysAppointments'
import { useAppointmentsStore } from '@/stores/appointments'
import type { Appointment } from '@/types/appointment'

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: 'property-1',
    propertyName: 'Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: '',
    category: 'maintenance',
    start: new Date(2026, 7, 10, 8, 30),
    end: new Date(2026, 7, 10, 10, 0),
    locked: false,
    recurring: false,
    recurrenceIntervalMonths: null,
    materials: [],
    history: [],
    travelDistanceKm: 0,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

/** Mounts a throwaway host component so the composable's onMounted-registered interval is actually active. */
function mountComposable() {
  let result!: ReturnType<typeof useTodaysAppointments>
  const wrapper = mount(
    defineComponent({
      setup() {
        result = useTodaysAppointments()
        return () => null
      },
    }),
  )
  return { wrapper, result }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 7, 10, 9, 0))
})

afterEach(() => {
  vi.useRealTimers()
})

describe('useTodaysAppointments', () => {
  it('returns an empty list when there are no appointments', () => {
    const { todaysAppointments } = useTodaysAppointments()

    expect(todaysAppointments.value).toEqual([])
  })

  it('excludes appointments scheduled on other days', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: '1', start: new Date(2026, 7, 9, 9, 0) }),
      createAppointment({ id: '2', start: new Date(2026, 7, 11, 9, 0) }),
    ]

    const { todaysAppointments } = useTodaysAppointments()

    expect(todaysAppointments.value).toEqual([])
  })

  it('sorts today\'s appointments from the earliest to the latest start time', () => {
    const store = useAppointmentsStore()
    const later = createAppointment({ id: '1', start: new Date(2026, 7, 10, 15, 30) })
    const earlier = createAppointment({ id: '2', start: new Date(2026, 7, 10, 8, 30) })
    store.appointments = [later, earlier]

    const { todaysAppointments } = useTodaysAppointments()

    expect(todaysAppointments.value.map((appointment) => appointment.id)).toEqual(['2', '1'])
  })

  it('includes both completed and not-yet-completed appointments scheduled for today', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: '1', completed: true }),
      createAppointment({ id: '2', completed: false }),
    ]

    const { todaysAppointments } = useTodaysAppointments()

    expect(todaysAppointments.value).toHaveLength(2)
  })

  it('returns tomorrow\'s appointments, sorted from the earliest to the latest start time', () => {
    const store = useAppointmentsStore()
    const later = createAppointment({ id: '1', start: new Date(2026, 7, 11, 15, 30) })
    const earlier = createAppointment({ id: '2', start: new Date(2026, 7, 11, 8, 30) })
    const today = createAppointment({ id: '3', start: new Date(2026, 7, 10, 9, 0) })
    store.appointments = [later, earlier, today]

    const { tomorrowsAppointments } = useTodaysAppointments()

    expect(tomorrowsAppointments.value.map((appointment) => appointment.id)).toEqual(['2', '1'])
  })

  it('is not "all completed" when there are no appointments today', () => {
    const { allTodaysAppointmentsCompleted } = useTodaysAppointments()

    expect(allTodaysAppointmentsCompleted.value).toBe(false)
  })

  it('is not "all completed" while at least one of today\'s appointments is still open', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: '1', completed: true }),
      createAppointment({ id: '2', completed: false }),
    ]

    const { allTodaysAppointmentsCompleted } = useTodaysAppointments()

    expect(allTodaysAppointmentsCompleted.value).toBe(false)
  })

  it('is "all completed" once every appointment today is completed', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: '1', completed: true }),
      createAppointment({ id: '2', completed: true }),
    ]

    const { allTodaysAppointmentsCompleted } = useTodaysAppointments()

    expect(allTodaysAppointmentsCompleted.value).toBe(true)
  })

  it('re-evaluates which day is "today" once the calendar day changes, without remounting', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: 'today', start: new Date(2026, 7, 10, 9, 0) }),
      createAppointment({ id: 'tomorrow', start: new Date(2026, 7, 11, 9, 0) }),
    ]
    const { wrapper, result } = mountComposable()

    expect(result.todaysAppointments.value.map((appointment) => appointment.id)).toEqual(['today'])
    expect(result.tomorrowsAppointments.value.map((appointment) => appointment.id)).toEqual([
      'tomorrow',
    ])

    vi.setSystemTime(new Date(2026, 7, 11, 0, 1))
    vi.advanceTimersByTime(60_000)

    expect(result.todaysAppointments.value.map((appointment) => appointment.id)).toEqual([
      'tomorrow',
    ])
    expect(result.tomorrowsAppointments.value).toEqual([])
    wrapper.unmount()
  })
})
