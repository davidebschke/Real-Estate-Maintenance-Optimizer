import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAppointmentsStore } from '@/stores/appointments'
import { useAuthStore } from '@/stores/auth'
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
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(appointmentService.fetchAppointments).mockReset()
  vi.mocked(appointmentService.createAppointment).mockReset()
  vi.mocked(appointmentService.updateAppointment).mockReset()
  vi.mocked(appointmentService.moveAppointment).mockReset()
  vi.mocked(appointmentService.completeAppointment).mockReset()
  vi.mocked(appointmentService.reopenAppointment).mockReset()
  vi.mocked(appointmentService.deleteAppointment).mockReset()
})

describe('useAppointmentsStore', () => {
  it('starts with no appointments and every overlay closed', () => {
    const store = useAppointmentsStore()

    expect(store.appointments).toEqual([])
    expect(store.isCreateDialogOpen).toBe(false)
    expect(store.editingAppointment).toBeNull()
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
    vi.mocked(appointmentService.createAppointment).mockResolvedValue({ status: 'created', appointment: created })
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([created])
    const store = useAppointmentsStore()

    const result = await store.createAppointment({
      title: created.title,
      propertyId: created.propertyId,
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

  it('records a creation conflict without creating an appointment, then clears it on the next successful create', async () => {
    const suggestedStart = new Date(2026, 7, 11, 15, 0)
    const suggestedEnd = new Date(2026, 7, 11, 16, 0)
    vi.mocked(appointmentService.createAppointment).mockResolvedValueOnce({
      status: 'conflict',
      message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
      suggestedStart,
      suggestedEnd,
    })
    const store = useAppointmentsStore()
    const payload = {
      title: 'Kellerreinigung Q3',
      propertyId: 'property-1',
      description: '',
      start: new Date(2026, 7, 11, 13, 0),
      durationMinutes: 120,
      locked: false,
      recurring: false,
      recurrenceIntervalMonths: null,
      materials: [],
    }

    const blockedResult = await store.createAppointment(payload)

    expect(blockedResult).toBeNull()
    expect(store.appointmentConflict).toEqual({
      message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
      suggestedStart,
      suggestedEnd,
    })
    expect(store.appointments).toEqual([])

    const created = createAppointment()
    vi.mocked(appointmentService.createAppointment).mockResolvedValueOnce({ status: 'created', appointment: created })
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([created])

    const succeededResult = await store.createAppointment(payload)

    expect(succeededResult).toEqual(created)
    expect(store.appointmentConflict).toBeNull()
  })

  it('updates an appointment, refreshes the list and clears a previous update error', async () => {
    const updated = createAppointment({ title: 'Fensterreinigung' })
    vi.mocked(appointmentService.updateAppointment).mockResolvedValue(updated)
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([updated])
    const store = useAppointmentsStore()
    store.hasUpdateError = true

    const succeeded = await store.updateAppointment('1', {
      title: updated.title,
      propertyId: updated.propertyId,
      description: updated.description,
      start: updated.start,
      durationMinutes: 120,
      locked: false,
      recurring: false,
      recurrenceIntervalMonths: null,
      materials: [],
    })

    expect(succeeded).toBe(true)
    expect(store.appointments).toEqual([updated])
    expect(store.hasUpdateError).toBe(false)
  })

  it('records an update failure without touching the local list', async () => {
    vi.mocked(appointmentService.updateAppointment).mockRejectedValue(new Error('network error'))
    const store = useAppointmentsStore()
    store.appointments = [createAppointment()]

    const succeeded = await store.updateAppointment('1', {
      title: 'Fensterreinigung',
      propertyId: 'property-1',
      description: '',
      start: new Date(2026, 7, 11, 13, 0),
      durationMinutes: 120,
      locked: false,
      recurring: false,
      recurrenceIntervalMonths: null,
      materials: [],
    })

    expect(succeeded).toBe(false)
    expect(store.hasUpdateError).toBe(true)
    expect(store.appointments).toEqual([createAppointment()])
  })

  it('clears a stale update error when opening the edit dialog for a (possibly different) appointment', () => {
    const store = useAppointmentsStore()
    store.hasUpdateError = true

    store.openEditDialog(createAppointment({ id: '2' }))

    expect(store.hasUpdateError).toBe(false)
  })

  it('opens and closes the edit dialog, and reflects both dialog modes through isFormDialogOpen', () => {
    const appointment = createAppointment()
    const store = useAppointmentsStore()

    expect(store.isFormDialogOpen).toBe(false)

    store.openEditDialog(appointment)
    expect(store.editingAppointment).toEqual(appointment)
    expect(store.isFormDialogOpen).toBe(true)

    store.closeEditDialog()
    expect(store.editingAppointment).toBeNull()
    expect(store.isFormDialogOpen).toBe(false)

    store.openCreateDialog()
    expect(store.isFormDialogOpen).toBe(true)

    store.isFormDialogOpen = false
    expect(store.isCreateDialogOpen).toBe(false)
  })

  it('moves an appointment and refreshes the list', async () => {
    const moved = createAppointment({ start: new Date(2026, 7, 12, 9, 0) })
    vi.mocked(appointmentService.moveAppointment).mockResolvedValue({ status: 'moved', appointment: moved })
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([moved])
    const store = useAppointmentsStore()

    const result = await store.moveAppointment('1', { start: moved.start, durationMinutes: 60 })

    expect(result).toEqual(moved)

    expect(appointmentService.moveAppointment).toHaveBeenCalledWith('1', {
      start: moved.start,
      durationMinutes: 60,
    })
    expect(store.appointments).toEqual([moved])
  })

  it('records a move conflict without moving or refreshing, then clears it on the next successful move', async () => {
    const suggestedStart = new Date(2026, 7, 11, 15, 15)
    const suggestedEnd = new Date(2026, 7, 11, 16, 15)
    vi.mocked(appointmentService.moveAppointment).mockResolvedValueOnce({
      status: 'conflict',
      message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
      suggestedStart,
      suggestedEnd,
    })
    const store = useAppointmentsStore()
    const payload = { start: new Date(2026, 7, 11, 14, 0), durationMinutes: 60 }

    const blockedResult = await store.moveAppointment('1', payload)

    expect(blockedResult).toBeNull()
    expect(store.appointmentConflict).toEqual({
      message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
      suggestedStart,
      suggestedEnd,
    })
    expect(appointmentService.fetchAppointments).not.toHaveBeenCalled()

    const moved = createAppointment({ start: new Date(2026, 7, 11, 15, 15) })
    vi.mocked(appointmentService.moveAppointment).mockResolvedValueOnce({ status: 'moved', appointment: moved })
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([moved])

    const succeededResult = await store.moveAppointment('1', { start: moved.start, durationMinutes: 60 })

    expect(succeededResult).toEqual(moved)
    expect(store.appointmentConflict).toBeNull()
  })

  it('clears a previous conflict when a detail view is opened', () => {
    const store = useAppointmentsStore()
    store.appointmentConflict = {
      message: 'Konflikt',
      suggestedStart: new Date(2026, 7, 11, 15, 15),
      suggestedEnd: new Date(2026, 7, 11, 16, 15),
    }

    store.openDetail('1')

    expect(store.appointmentConflict).toBeNull()
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

  it('refreshes the remaining creation limit of a demo account after creating an appointment', async () => {
    vi.mocked(appointmentService.createAppointment).mockResolvedValue({
      status: 'created',
      appointment: createAppointment(),
    })
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([createAppointment()])
    const refreshDemoQuota = vi.spyOn(useAuthStore(), 'refreshDemoQuota').mockResolvedValue()
    const store = useAppointmentsStore()

    await store.createAppointment({
      title: 'Kellerreinigung Q3',
      propertyId: 'property-1',
      description: '',
      start: new Date(2026, 7, 11, 13, 0),
      durationMinutes: 120,
      locked: false,
      recurring: false,
      recurrenceIntervalMonths: null,
      materials: [],
    })

    expect(refreshDemoQuota).toHaveBeenCalledOnce()
  })

  it('forgets every appointment and closes every overlay on reset', () => {
    const store = useAppointmentsStore()
    store.appointments = [createAppointment()]
    store.openCreateDialog()
    store.openEditDialog(createAppointment())
    store.hasUpdateError = true
    store.appointmentConflict = {
      message: 'Konflikt',
      suggestedStart: new Date(2026, 7, 11, 15, 0),
      suggestedEnd: new Date(2026, 7, 11, 16, 0),
    }
    store.openDetail('1')

    store.reset()

    expect(store.appointments).toEqual([])
    expect(store.isCreateDialogOpen).toBe(false)
    expect(store.editingAppointment).toBeNull()
    expect(store.hasUpdateError).toBe(false)
    expect(store.appointmentConflict).toBeNull()
    expect(store.activeDetailAppointmentId).toBeNull()
  })

  it('discards a fetch that is answered only after an account change', async () => {
    let resolveFetch: (appointments: Appointment[]) => void = () => {}
    vi.mocked(appointmentService.fetchAppointments).mockReturnValue(new Promise((resolve) => (resolveFetch = resolve)))
    const store = useAppointmentsStore()
    const pendingFetch = store.fetchAppointments()

    store.reset()
    resolveFetch([createAppointment({ id: 'from-previous-account' })])
    await pendingFetch

    expect(store.appointments).toEqual([])
  })

  it('keeps only the result of the latest of two overlapping fetches', async () => {
    let resolveFirst: (appointments: Appointment[]) => void = () => {}
    vi.mocked(appointmentService.fetchAppointments)
      .mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockResolvedValueOnce([createAppointment({ id: 'latest' })])
    const store = useAppointmentsStore()
    const firstFetch = store.fetchAppointments()
    await store.fetchAppointments()

    resolveFirst([createAppointment({ id: 'outdated' })])
    await firstFetch

    expect(store.appointments.map((appointment) => appointment.id)).toEqual(['latest'])
  })
})
