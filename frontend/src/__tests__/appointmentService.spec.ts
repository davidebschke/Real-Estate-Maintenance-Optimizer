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
} from '@/services/appointmentService'
import type { AppointmentResponseDto } from '@/services/appointmentService'

vi.mock('axios')

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

    await createAppointment({
      title: 'Kellerreinigung Q3',
      propertyId: 'property-1',
      propertyName: 'Wohnanlage Sonnenhof',
      propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
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
  })

  it('moves an appointment to a new start and duration', async () => {
    vi.mocked(axios.patch).mockResolvedValue({ data: createDto() })

    await moveAppointment('1', { start: new Date(2026, 7, 12, 9, 0), durationMinutes: 60 })

    expect(axios.patch).toHaveBeenCalledWith(
      expect.stringContaining('/api/appointments/1/schedule'),
      { start: '2026-08-12T09:00:00', durationMinutes: 60 },
    )
  })

  it('completes an appointment', async () => {
    vi.mocked(axios.patch).mockResolvedValue({ data: createDto({ completed: true, actualEnd: '2026-08-11T14:30:00' }) })

    const appointment = await completeAppointment('1')

    expect(axios.patch).toHaveBeenCalledWith(expect.stringContaining('/api/appointments/1/complete'))
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
