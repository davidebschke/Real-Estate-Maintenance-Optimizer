/** How confidently the backend assigned a scanned payment to a tenant. */
export type PaymentMatch = 'MATCH' | 'POSSIBLE' | 'NONE'

/** The payment state of one month of a tenant's rent. */
export type MonthPaymentStatus = 'PAID' | 'PARTIAL' | 'PENDING' | 'OVERDUE' | 'UPCOMING' | 'UNTRACKED'

/** The worst payment state of a tenant over every tracked month up to today. */
export type PaymentAlert = 'NONE' | 'PENDING' | 'OVERDUE'

/** A payment alert that actually needs attention. */
export type ActivePaymentAlert = Exclude<PaymentAlert, 'NONE'>

/** A payment the AI found on a scanned bank statement, with its suggested tenant assignment. */
export interface ScannedPayment {
  /** ISO date (YYYY-MM-DD) the money arrived. */
  paymentDate: string
  amount: number
  payerName: string
  reference: string
  periodYear: number
  /** 1 to 12, the month the rent is paid for. */
  periodMonth: number
  tenantId: string | null
  match: PaymentMatch
  /** Whether an identical payment was already saved before. */
  alreadyRecorded: boolean
}

/** A payment the user confirmed in the preview and wants to save for a tenant. */
export interface PaymentToSave {
  tenantId: string
  paymentDate: string
  amount: number
  reference: string
  periodYear: number
  periodMonth: number
}

/** The outcome of saving confirmed payments. */
export interface SavePaymentsResult {
  savedCount: number
  skippedCount: number
}

/** The state of one month in a tenant's year table. */
export interface MonthPayment {
  /** 1 to 12. */
  month: number
  status: MonthPaymentStatus
  paidAmount: number
}

/** One tenant's row of the year table with the payment state of all twelve months. */
export interface TenantPaymentYear {
  tenantId: string
  apartmentId: string
  firstName: string
  lastName: string
  floor: number
  monthlyRent: number
  alert: PaymentAlert
  months: MonthPayment[]
}

/** The rent payment table of a property for one year. */
export interface PaymentYearOverview {
  year: number
  tenants: TenantPaymentYear[]
}

/** A tenant with a pending or overdue payment, account-wide. */
export interface PaymentAlertEntry {
  propertyId: string
  apartmentId: string
  tenantId: string
  alert: ActivePaymentAlert
}

/** A scanned bank statement waiting for the user's review before anything is saved. */
export interface ScanSession {
  propertyId: string
  payments: ScannedPayment[]
}
