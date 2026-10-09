/** The logged-in account, including a demo account's expiry, remaining creation limits (null when unlimited) and the minimum gap in minutes kept between appointments. */
export interface CurrentUser {
  username: string
  displayName: string
  demoAccount: boolean
  expiresAt: Date | null
  remainingPropertyCreations: number | null
  remainingAppointmentCreations: number | null
  remainingTenantCreations: number | null
  appointmentBufferMinutes: number
}

/** Resource kind a demo account may only create a limited number of. */
export type DemoQuotaResource = 'properties' | 'appointments' | 'tenants'

/** Why a login or demo account request failed, mapped to a translatable message. */
export type AuthFailureReason = 'invalidCredentials' | 'tooManyRequests' | 'generic'

/** Why changing a profile setting failed, mapped to a translatable message. */
export type AccountFailureReason =
  | 'usernameTaken'
  | 'currentPasswordIncorrect'
  | 'invalidInput'
  | 'tooManyRequests'
  | 'generic'
