/** The logged-in account, including a demo account's expiry and remaining creation limits (null when unlimited). */
export interface CurrentUser {
  username: string
  displayName: string
  demoAccount: boolean
  expiresAt: Date | null
  remainingPropertyCreations: number | null
  remainingAppointmentCreations: number | null
}

/** Resource kind a demo account may only create a limited number of. */
export type DemoQuotaResource = 'properties' | 'appointments'

/** Why a login or demo account request failed, mapped to a translatable message. */
export type AuthFailureReason = 'invalidCredentials' | 'tooManyRequests' | 'generic'
