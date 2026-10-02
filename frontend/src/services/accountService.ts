import axios from 'axios'
import { toCurrentUser, type CurrentUserResponseDto } from '@/services/authService'
import type { AccountFailureReason, CurrentUser } from '@/types/auth'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/** Changes the username of the logged-in account and returns the updated account. */
export async function changeUsername(username: string): Promise<CurrentUser> {
  const { data } = await axios.put<CurrentUserResponseDto>(`${apiBaseUrl}/api/account/username`, { username })
  return toCurrentUser(data)
}

/** Changes the password after the backend verified the current one; the response carries a fresh session cookie. */
export async function changePassword(currentPassword: string, newPassword: string): Promise<CurrentUser> {
  const { data } = await axios.put<CurrentUserResponseDto>(`${apiBaseUrl}/api/account/password`, {
    currentPassword,
    newPassword,
  })
  return toCurrentUser(data)
}

/** Changes the minimum gap in minutes kept between two appointments of the logged-in account. */
export async function changeAppointmentBuffer(appointmentBufferMinutes: number): Promise<CurrentUser> {
  const { data } = await axios.put<CurrentUserResponseDto>(`${apiBaseUrl}/api/account/appointment-buffer`, {
    appointmentBufferMinutes,
  })
  return toCurrentUser(data)
}

/** Maps a failed profile setting request to the reason shown to the user. */
export function toAccountFailureReason(error: unknown): AccountFailureReason {
  if (!axios.isAxiosError(error)) return 'generic'
  switch (error.response?.status) {
    case 409:
      return 'usernameTaken'
    case 422:
      return 'currentPasswordIncorrect'
    case 400:
      return 'invalidInput'
    case 429:
      return 'tooManyRequests'
    default:
      return 'generic'
  }
}
