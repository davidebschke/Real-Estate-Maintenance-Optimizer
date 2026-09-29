import axios from 'axios'
import type { AuthFailureReason, CurrentUser } from '@/types/auth'

/** Shape of the logged-in account as returned by the backend. */
export interface CurrentUserResponseDto {
  username: string
  displayName: string
  demoAccount: boolean
  expiresAt: string | null
  remainingPropertyCreations: number | null
  remainingAppointmentCreations: number | null
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/** Fetches the logged-in account, or null when there is no valid session. */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  try {
    const { data } = await axios.get<CurrentUserResponseDto>(`${apiBaseUrl}/api/auth/me`)
    return toCurrentUser(data)
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) return null
    throw error
  }
}

/** Logs in with the given credentials, starting a session held in an HttpOnly cookie. */
export async function login(username: string, password: string): Promise<CurrentUser> {
  const { data } = await axios.post<CurrentUserResponseDto>(`${apiBaseUrl}/api/auth/login`, {
    username,
    password,
  })
  return toCurrentUser(data)
}

/** Creates a session-only demo account pre-filled with example data and starts its session. */
export async function createDemoAccount(): Promise<CurrentUser> {
  const { data } = await axios.post<CurrentUserResponseDto>(`${apiBaseUrl}/api/auth/demo-account`)
  return toCurrentUser(data)
}

/** Ends the current session; the backend deletes a demo account together with all its data. */
export async function logout(): Promise<void> {
  await axios.post(`${apiBaseUrl}/api/auth/logout`)
}

/** Maps a failed login or demo account request to the reason shown to the user. */
export function toAuthFailureReason(error: unknown): AuthFailureReason {
  if (!axios.isAxiosError(error)) return 'generic'
  if (error.response?.status === 401) return 'invalidCredentials'
  if (error.response?.status === 429) return 'tooManyRequests'
  return 'generic'
}

/** Converts the backend DTO into the frontend's domain shape. */
function toCurrentUser(dto: CurrentUserResponseDto): CurrentUser {
  return {
    username: dto.username,
    displayName: dto.displayName,
    demoAccount: dto.demoAccount,
    expiresAt: dto.expiresAt ? new Date(dto.expiresAt) : null,
    remainingPropertyCreations: dto.remainingPropertyCreations,
    remainingAppointmentCreations: dto.remainingAppointmentCreations,
  }
}
