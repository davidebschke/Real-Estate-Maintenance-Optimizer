import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAppointmentsStore } from '@/stores/appointments'
import * as appointmentService from '@/services/appointmentService'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/appointmentService')

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Kellerreinigung Q3',
    propertyId: 'property-1',
    propertyName: 'Wohnanlage Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: 'Was ist zu tun?',
    category: 'maintenance',
    start: new Date(2026, 7, 11, 13, 0),
    end: new Date(2026, 7, 11, 15, 0),
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

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(appointmentService.fetchAppointments).mockReset()
  vi.mocked(appointmentService.createAppointment).mockReset()
  vi.mocked(appointmentService.moveAppointment).mockReset()
  vi.mocked(appointmentService.completeAppointment).mockReset()
  vi.mocked(appointmentService.reopenAppointment).mockReset()
  vi.mocked(appointmentService.deleteAppointment).mockReset()
})

describe('useAppointmentsStore', () => {
  it('starts with no appointments and both overlays closed', () => {
    const store = useAppointmentsStore()

    expect(store.appointments).toEqual([])
    expect(store.isCreateDialogOpen).toBe(false)
    expect(store.activeDetailAppointmentId).toBeNull()
  })

  it('fetches appointments from the backend', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([createAppointment()])
    const store = useAppointmentsStore()

    await store.fetchAppointments()

    expect(store.appointments).toHaveLength(1)
  })

  it('creates an appointment and refreshes the list', async () => {
    const created = createAppointment()
    vi.mocked(appointmentService.createAppointment).mockResolvedValue(created)
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([created])
    const store = useAppointmentsStore()

    const result = await store.createAppointment({
      title: created.title,
      propertyId: created.propertyId,
      propertyName: created.propertyName,
      propertyAddress: created.propertyAddress,
      description: created.description,
      start: created.start,
      durationMinutes: 120,
      locked: false,
      recurring: false,
      recurrenceIntervalMonths: null,
      materials: [],
    })

    expect(result).toEqual(created)
    expect(store.appointments).toEqual([created])
  })

  it('moves an appointment and refreshes the list', async () => {
    const moved = createAppointment({ start: new Date(2026, 7, 12, 9, 0) })
    vi.mocked(appointmentService.moveAppointment).mockResolvedValue(moved)
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([moved])
    const store = useAppointmentsStore()

    await store.moveAppointment('1', { start: moved.start, durationMinutes: 60 })

    expect(appointmentService.moveAppointment).toHaveBeenCalledWith('1', {
      start: moved.start,
      durationMinutes: 60,
    })
    expect(store.appointments).toEqual([moved])
  })

  it('completes an appointment and refreshes the list', async () => {
    const completed = createAppointment({ completed: true, actualEnd: new Date(2026, 7, 11, 14, 30) })
    vi.mocked(appointmentService.completeAppointment).mockResolvedValue(completed)
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([completed])
    const store = useAppointmentsStore()

    const actualEnd = new Date(2026, 7, 11, 14, 30)
    await store.completeAppointment('1', actualEnd)

    expect(appointmentService.completeAppointment).toHaveBeenCalledWith('1', actualEnd)
    expect(store.appointments).toEqual([completed])
  })

  it('reopens a completed appointment and refreshes the list', async () => {
    const reopened = createAppointment({ completed: false, actualEnd: null })
    vi.mocked(appointmentService.reopenAppointment).mockResolvedValue(reopened)
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([reopened])
    const store = useAppointmentsStore()

    await store.reopenAppointment('1')

    expect(appointmentService.reopenAppointment).toHaveBeenCalledWith('1')
    expect(store.appointments).toEqual([reopened])
  })

  it('deletes an appointment with the given scope and refreshes the list', async () => {
    vi.mocked(appointmentService.deleteAppointment).mockResolvedValue(undefined)
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
    const store = useAppointmentsStore()

    await store.deleteAppointment('1', 'series')

    expect(appointmentService.deleteAppointment).toHaveBeenCalledWith('1', 'series')
    expect(store.appointments).toEqual([])
  })

  it('opens and closes the create dialog', () => {
    const store = useAppointmentsStore()

    store.openCreateDialog()
    expect(store.isCreateDialogOpen).toBe(true)

    store.closeCreateDialog()
    expect(store.isCreateDialogOpen).toBe(false)
  })

  it('opens and closes the detail view for a given appointment id', () => {
    const store = useAppointmentsStore()

    store.openDetail('1')
    expect(store.activeDetailAppointmentId).toBe('1')

    store.closeDetail()
    expect(store.activeDetailAppointmentId).toBeNull()
  })
})
