import axios from 'axios'
import type { Appointment } from '@/types/appointment'

/** Shape of a history entry as returned by the backend. */
export interface AppointmentHistoryEntryDto {
  timestamp: string
  message: string
}

/** Shape of an appointment as returned by the backend. */
export interface AppointmentResponseDto {
  id: string
  seriesId: string | null
  title: string
  propertyId: string
  propertyName: string
  propertyAddress: string
  description: string
  start: string
  end: string
  locked: boolean
  recurring: boolean
  recurrenceIntervalMonths: number | null
  materials: string[]
  history: AppointmentHistoryEntryDto[]
  actualEnd: string | null
  completed: boolean
}

/** Payload for creating a new appointment. */
export interface CreateAppointmentPayload {
  title: string
  propertyId: string
  propertyName: string
  propertyAddress: string
  description: string
  start: Date
  durationMinutes: number
  locked: boolean
  recurring: boolean
  recurrenceIntervalMonths: number | null
  materials: string[]
}

/** Payload for rescheduling an existing, unlocked appointment. */
export interface MoveAppointmentPayload {
  start: Date
  durationMinutes: number
}

/** Scope of a delete operation: only the given appointment, or it and every following occurrence of its recurring series. */
export type AppointmentDeleteScope = 'single' | 'series'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/** Fetches every appointment, sorted by start time. */
export async function fetchAppointments(): Promise<Appointment[]> {
  const { data } = await axios.get<AppointmentResponseDto[]>(`${apiBaseUrl}/api/appointments`)
  return data.map(toAppointment)
}

/** Fetches a single appointment by id. */
export async function fetchAppointment(id: string): Promise<Appointment> {
  const { data } = await axios.get<AppointmentResponseDto>(`${apiBaseUrl}/api/appointments/${id}`)
  return toAppointment(data)
}

/** Creates a new appointment, returning its first (or only) occurrence. */
export async function createAppointment(payload: CreateAppointmentPayload): Promise<Appointment> {
  const { data } = await axios.post<AppointmentResponseDto>(`${apiBaseUrl}/api/appointments`, {
    ...payload,
    start: toLocalDateTimeString(payload.start),
  })
  return toAppointment(data)
}

/** Reschedules an unlocked appointment to a new start and duration. */
export async function moveAppointment(
  id: string,
  payload: MoveAppointmentPayload,
): Promise<Appointment> {
  const { data } = await axios.patch<AppointmentResponseDto>(
    `${apiBaseUrl}/api/appointments/${id}/schedule`,
    {
      start: toLocalDateTimeString(payload.start),
      durationMinutes: payload.durationMinutes,
    },
  )
  return toAppointment(data)
}

/** Marks an appointment as completed with the given actual end time. */
export async function completeAppointment(id: string, actualEnd: Date): Promise<Appointment> {
  const { data } = await axios.patch<AppointmentResponseDto>(
    `${apiBaseUrl}/api/appointments/${id}/complete`,
    { actualEnd: toLocalDateTimeString(actualEnd) },
  )
  return toAppointment(data)
}

/** Reverts a completed appointment back to its not-yet-completed state. */
export async function reopenAppointment(id: string): Promise<Appointment> {
  const { data } = await axios.patch<AppointmentResponseDto>(
    `${apiBaseUrl}/api/appointments/${id}/reopen`,
  )
  return toAppointment(data)
}

/** Deletes an appointment; scope "series" also deletes every not-yet-past occurrence of its recurring series. */
export async function deleteAppointment(
  id: string,
  scope: AppointmentDeleteScope = 'single',
): Promise<void> {
  await axios.delete(`${apiBaseUrl}/api/appointments/${id}`, { params: { scope } })
}

/** Converts a backend appointment DTO into the frontend's domain shape, defaulting fields the create form has no control for. */
function toAppointment(dto: AppointmentResponseDto): Appointment {
  return {
    id: dto.id,
    seriesId: dto.seriesId,
    title: dto.title,
    propertyId: dto.propertyId,
    propertyName: dto.propertyName,
    propertyAddress: dto.propertyAddress,
    description: dto.description,
    category: 'maintenance',
    start: new Date(dto.start),
    end: new Date(dto.end),
    locked: dto.locked,
    recurring: dto.recurring,
    recurrenceIntervalMonths: dto.recurrenceIntervalMonths,
    materials: dto.materials,
    history: dto.history.map((entry) => ({
      timestamp: new Date(entry.timestamp),
      message: entry.message,
    })),
    travelDistanceKm: 0,
    actualEnd: dto.actualEnd ? new Date(dto.actualEnd) : null,
    completed: dto.completed,
  }
}

/** Formats a `Date` as the `YYYY-MM-DDTHH:mm:ss` string the backend's `LocalDateTime` fields expect. */
function toLocalDateTimeString(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}
