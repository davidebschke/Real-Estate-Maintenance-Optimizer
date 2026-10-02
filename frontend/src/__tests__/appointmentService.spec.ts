import { afterEach, describe, it, expect, vi } from 'vitest'
import axios from 'axios'
import {
  completeAppointment,
  createAppointment,
  deleteAppointment,
  fetchAppointment,
  fetchAppointments,
  moveAppointment,
  reopenAppointment,
  updateAppointment,
} from '@/services/appointmentService'
import type { AppointmentResponseDto } from '@/services/appointmentService'
import { httpError } from '@/__tests__/httpError'

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('axios')>()
  return {
    ...actual,
    default: {
      ...actual.default,
      get: vi.fn<typeof actual.default.get>(),
      post: vi.fn<typeof actual.default.post>(),
      put: vi.fn<typeof actual.default.put>(),
      patch: vi.fn<typeof actual.default.patch>(),
      delete: vi.fn<typeof actual.default.delete>(),
      isAxiosError: actual.isAxiosError,
    },
  }
})

/** Builds a sample backend appointment DTO for tests, with overridable fields. */
function createDto(overrides: Partial<AppointmentResponseDto> = {}): AppointmentResponseDto {
  return {
    id: '1',
    seriesId: null,
    title: 'Kellerreinigung Q3',
    propertyId: 'property-1',
    propertyName: 'Wohnanlage Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: 'Was ist zu tun?',
    start: '2026-08-11T13:00:00',
    end: '2026-08-11T15:00:00',
    locked: false,
    recurring: false,
    recurrenceIntervalMonths: null,
    materials: ['Kehrmaschine'],
    history: [{ timestamp: '2026-08-01T10:00:00Z', message: 'Termin erstellt.' }],
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

afterEach(() => {
  vi.mocked(axios.get).mockReset()
  vi.mocked(axios.post).mockReset()
  vi.mocked(axios.put).mockReset()
  vi.mocked(axios.patch).mockReset()
  vi.mocked(axios.delete).mockReset()
})

describe('appointmentService', () => {
  it('fetches and maps every appointment', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [createDto()] })

    const appointments = await fetchAppointments()

    expect(appointments).toEqual([
      expect.objectContaining({
        id: '1',
        title: 'Kellerreinigung Q3',
        start: new Date('2026-08-11T13:00:00'),
        end: new Date('2026-08-11T15:00:00'),
        category: 'maintenance',
        travelDistanceKm: 0,
      }),
    ])
    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/api/appointments'))
  })

  it('fetches a single appointment by id', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: createDto({ id: '42' }) })

    const appointment = await fetchAppointment('42')

    expect(appointment.id).toBe('42')
    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/api/appointments/42'))
  })

  it('creates an appointment, sending the start as a local date-time string', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: createDto() })

    const result = await createAppointment({
      title: 'Kellerreinigung Q3',
      propertyId: 'property-1',
      description: 'Was ist zu tun?',
      start: new Date(2026, 7, 11, 13, 0),
      durationMinutes: 120,
      locked: false,
      recurring: false,
      recurrenceIntervalMonths: null,
      materials: ['Kehrmaschine'],
    })

    expect(axios.post).toHaveBeenCalledWith(
      expect.stringContaining('/api/appointments'),
      expect.objectContaining({ start: '2026-08-11T13:00:00' }),
    )
    expect(result).toEqual(expect.objectContaining({ status: 'created' }))
  })

  it('returns the suggested next free slot when creation is blocked by a 409 conflict', async () => {
    vi.mocked(axios.post).mockRejectedValue(
      httpError(409, {
        message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
        suggestedStart: '2026-08-11T15:00:00',
        suggestedEnd: '2026-08-11T16:00:00',
      }),
    )

    const result = await createAppointment({
      title: 'Kellerreinigung Q3',
      propertyId: 'property-1',
      description: '',
      start: new Date(2026, 7, 11, 13, 0),
      durationMinutes: 60,
      locked: false,
      recurring: false,
      recurrenceIntervalMonths: null,
      materials: [],
    })

    expect(result).toEqual({
      status: 'conflict',
      message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
      suggestedStart: new Date('2026-08-11T15:00:00'),
      suggestedEnd: new Date('2026-08-11T16:00:00'),
    })
  })

  it('rethrows an error that is not a 409 conflict response', async () => {
    const serverError = httpError(500)
    vi.mocked(axios.post).mockRejectedValue(serverError)

    await expect(
      createAppointment({
        title: 'Kellerreinigung Q3',
        propertyId: 'property-1',
        description: '',
        start: new Date(2026, 7, 11, 13, 0),
        durationMinutes: 60,
        locked: false,
        recurring: false,
        recurrenceIntervalMonths: null,
        materials: [],
      }),
    ).rejects.toBe(serverError)
  })

  it('updates an appointment, sending the start as a local date-time string', async () => {
    vi.mocked(axios.put).mockResolvedValue({ data: createDto({ title: 'Fensterreinigung' }) })

    const appointment = await updateAppointment('1', {
      title: 'Fensterreinigung',
      propertyId: 'property-1',
      description: 'Neue Beschreibung',
      start: new Date(2026, 7, 11, 13, 0),
      durationMinutes: 90,
      locked: false,
      recurring: false,
      recurrenceIntervalMonths: null,
      materials: ['Fensterwischer'],
    })

    expect(axios.put).toHaveBeenCalledWith(
      expect.stringContaining('/api/appointments/1'),
      expect.objectContaining({ start: '2026-08-11T13:00:00', title: 'Fensterreinigung' }),
    )
    expect(appointment.title).toBe('Fensterreinigung')
  })

  it('moves an appointment to a new start and duration', async () => {
    vi.mocked(axios.patch).mockResolvedValue({ data: createDto() })

    const result = await moveAppointment('1', { start: new Date(2026, 7, 12, 9, 0), durationMinutes: 60 })

    expect(axios.patch).toHaveBeenCalledWith(
      expect.stringContaining('/api/appointments/1/schedule'),
      { start: '2026-08-12T09:00:00', durationMinutes: 60 },
    )
    expect(result).toEqual(expect.objectContaining({ status: 'moved' }))
  })

  it('returns the suggested next free slot when a move is blocked by a 409 conflict', async () => {
    vi.mocked(axios.patch).mockRejectedValue(
      httpError(409, {
        message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
        suggestedStart: '2026-08-11T15:15:00',
        suggestedEnd: '2026-08-11T16:15:00',
      }),
    )

    const result = await moveAppointment('1', { start: new Date(2026, 7, 11, 14, 0), durationMinutes: 60 })

    expect(result).toEqual({
      status: 'conflict',
      message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
      suggestedStart: new Date('2026-08-11T15:15:00'),
      suggestedEnd: new Date('2026-08-11T16:15:00'),
    })
  })

  it('rethrows a move error that is not a slot conflict, such as a locked appointment', async () => {
    const lockedError = httpError(409, { message: 'Der Termin ist unverschiebbar.' })
    vi.mocked(axios.patch).mockRejectedValue(lockedError)

    await expect(
      moveAppointment('1', { start: new Date(2026, 7, 12, 9, 0), durationMinutes: 60 }),
    ).rejects.toBe(lockedError)
  })

  it('completes an appointment with the given actual end time', async () => {
    vi.mocked(axios.patch).mockResolvedValue({ data: createDto({ completed: true, actualEnd: '2026-08-11T14:30:00' }) })

    const appointment = await completeAppointment('1', new Date(2026, 7, 11, 14, 30))

    expect(axios.patch).toHaveBeenCalledWith(
      expect.stringContaining('/api/appointments/1/complete'),
      { actualEnd: '2026-08-11T14:30:00' },
    )
    expect(appointment.completed).toBe(true)
    expect(appointment.actualEnd).toEqual(new Date('2026-08-11T14:30:00'))
  })

  it('reopens a completed appointment', async () => {
    vi.mocked(axios.patch).mockResolvedValue({ data: createDto() })

    const appointment = await reopenAppointment('1')

    expect(axios.patch).toHaveBeenCalledWith(expect.stringContaining('/api/appointments/1/reopen'))
    expect(appointment.completed).toBe(false)
  })

  it('deletes an appointment with the given scope', async () => {
    vi.mocked(axios.delete).mockResolvedValue({})

    await deleteAppointment('1', 'series')

    expect(axios.delete).toHaveBeenCalledWith(
      expect.stringContaining('/api/appointments/1'),
      expect.objectContaining({ params: { scope: 'series' } }),
    )
  })
})
