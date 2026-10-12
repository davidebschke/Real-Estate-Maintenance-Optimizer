import axios from 'axios'
import type {
  PaymentAlertEntry,
  PaymentToSave,
  PaymentYearOverview,
  SavePaymentsResult,
  ScannedPayment,
} from '@/types/payment'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/** Sends a bank statement PDF to the backend, which has the AI read its payments without storing the file. */
export async function scanStatement(propertyId: string, file: File): Promise<ScannedPayment[]> {
  const body = new FormData()
  body.append('file', file)
  const { data } = await axios.post<{ payments: ScannedPayment[] }>(
    `${apiBaseUrl}/api/properties/${propertyId}/rent-payments/scan`,
    body,
  )
  return data.payments
}

/** Saves the payments the user confirmed in the preview for their tenants. */
export async function savePayments(propertyId: string, payments: PaymentToSave[]): Promise<SavePaymentsResult> {
  const { data } = await axios.post<SavePaymentsResult>(`${apiBaseUrl}/api/properties/${propertyId}/rent-payments`, {
    payments,
  })
  return data
}

/** Fetches the rent payment table of the given property for one year. */
export async function fetchPaymentYear(propertyId: string, year: number): Promise<PaymentYearOverview> {
  const { data } = await axios.get<PaymentYearOverview>(`${apiBaseUrl}/api/properties/${propertyId}/rent-payments`, {
    params: { year },
  })
  return data
}

/** Fetches every tenant of the logged-in account with a pending or overdue rent payment. */
export async function fetchPaymentAlerts(): Promise<PaymentAlertEntry[]> {
  const { data } = await axios.get<PaymentAlertEntry[]>(`${apiBaseUrl}/api/rent-payments/alerts`)
  return data
}
