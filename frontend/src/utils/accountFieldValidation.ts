/** Longest appointment buffer in minutes the backend accepts (one day). */
export const MAX_APPOINTMENT_BUFFER_MINUTES = 24 * 60

/** Shortest accepted password, in characters. */
export const PASSWORD_MIN_LENGTH = 8

/** Longest accepted password, in UTF-8 bytes (the limit of BCrypt). */
export const PASSWORD_MAX_BYTES = 72

const USERNAME_PATTERN = /^(?!demo-)[A-Za-z0-9._-]{3,50}$/i

/** Why a new password is not acceptable. */
export type NewPasswordProblem = 'tooShort' | 'tooLong' | 'unchanged'

/** Whether the given username consists of 3 to 50 letters, digits, dots, hyphens or underscores and does not start with the prefix reserved for demo accounts. */
export function isValidUsername(username: string): boolean {
  return USERNAME_PATTERN.test(username)
}

/** Returns why the new password cannot be used given the current one, or null when it is acceptable. */
export function findNewPasswordProblem(newPassword: string, currentPassword: string): NewPasswordProblem | null {
  if (newPassword.length < PASSWORD_MIN_LENGTH) return 'tooShort'
  if (new TextEncoder().encode(newPassword).length > PASSWORD_MAX_BYTES) return 'tooLong'
  if (newPassword === currentPassword) return 'unchanged'
  return null
}

/** Whether the given value is a whole number of minutes the backend accepts as appointment buffer. */
export function isValidAppointmentBuffer(minutes: number | null): minutes is number {
  return minutes !== null && Number.isInteger(minutes) && minutes >= 0 && minutes <= MAX_APPOINTMENT_BUFFER_MINUTES
}
