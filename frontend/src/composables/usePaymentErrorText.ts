import { useI18n } from 'vue-i18n'
import type { PaymentRequestError } from '@/stores/payments'

/** Statuses of the rent payment endpoints that have their own localized fallback text. */
const KNOWN_STATUSES = [400, 404, 409, 413, 429, 501, 503]

/** Provides the text to show for a failed scan or save request. */
export function usePaymentErrorText() {
  const { t } = useI18n()

  /** Returns the backend's own localized message, else a text for the HTTP status, else the given generic text. */
  function describeError(error: PaymentRequestError, genericKey: string): string {
    if (error.message) return error.message
    if (error.status !== null && KNOWN_STATUSES.includes(error.status)) return t(`payments.errors.status${error.status}`)
    return t(genericKey)
  }

  return { describeError }
}
