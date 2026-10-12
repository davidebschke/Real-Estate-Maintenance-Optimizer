import { computed, ref } from 'vue'
import axios from 'axios'
import { defineStore } from 'pinia'
import * as paymentService from '@/services/paymentService'
import { getServerErrorMessage } from '@/utils/serverErrorMessage'
import type {
  ActivePaymentAlert,
  PaymentAlert,
  PaymentAlertEntry,
  PaymentToSave,
  PaymentYearOverview,
  SavePaymentsResult,
  ScanSession,
} from '@/types/payment'

/** Why a scan or save request failed: the HTTP status (null without a response) and the localized message the backend gave, if any. */
export interface PaymentRequestError {
  status: number | null
  message: string | null
}

/** Holds the rent payment tables per property, the account-wide payment alerts and the state of the bank statement scan with its preview. */
export const usePaymentsStore = defineStore('payments', () => {
  const selectedYear = ref(new Date().getFullYear())
  const overviewsByProperty = ref<Record<string, PaymentYearOverview>>({})
  const hasYearLoadError = ref(false)
  const isYearLoading = ref(false)
  const alerts = ref<PaymentAlertEntry[]>([])
  const hasAlertsLoadError = ref(false)
  const isScanning = ref(false)
  const scanError = ref<PaymentRequestError | null>(null)
  const scanSession = ref<ScanSession | null>(null)
  const isSaving = ref(false)
  const saveError = ref<PaymentRequestError | null>(null)
  const isActivating = ref(false)
  const activationError = ref<PaymentRequestError | null>(null)
  /** Bumped by every year fetch and by reset(), so an answer to an outdated year request is discarded. */
  let latestYearToken = 0
  /** Bumped by every alerts fetch and by reset(), so an answer to an outdated alerts request is discarded. */
  let latestAlertsToken = 0
  /** Bumped by reset(), so a scan or save answered only after the account changed never touches the new account's data. */
  let sessionEpoch = 0

  const alertByTenant = computed<Record<string, ActivePaymentAlert>>(() =>
    Object.fromEntries(alerts.value.map((entry) => [entry.tenantId, entry.alert])),
  )

  /** Whether the scan preview should be visible; closing it discards the scanned payments. */
  const isPreviewOpen = computed({
    get: () => scanSession.value !== null,
    set: (value: boolean) => {
      if (!value) discardScan()
    },
  })

  /** Returns the worst payment alert among the tenants of the given property, or `NONE` if none has one. */
  function alertForProperty(propertyId: string): PaymentAlert {
    const propertyAlerts = alerts.value.filter((entry) => entry.propertyId === propertyId)
    if (propertyAlerts.some((entry) => entry.alert === 'OVERDUE')) return 'OVERDUE'
    return propertyAlerts.length > 0 ? 'PENDING' : 'NONE'
  }

  /** Loads the payment table of the given property for the selected year, recording whether the request failed; ignores the result if a newer fetch or an account change already happened. */
  async function fetchYear(propertyId: string) {
    const requestToken = ++latestYearToken
    isYearLoading.value = true
    try {
      const fetched = await paymentService.fetchPaymentYear(propertyId, selectedYear.value)
      if (requestToken !== latestYearToken) return
      overviewsByProperty.value = { ...overviewsByProperty.value, [propertyId]: fetched }
      hasYearLoadError.value = false
    } catch {
      if (requestToken !== latestYearToken) return
      hasYearLoadError.value = true
    } finally {
      if (requestToken === latestYearToken) isYearLoading.value = false
    }
  }

  /** Selects another year and loads the payment table of the given property for it. */
  function changeYear(propertyId: string, year: number): Promise<void> {
    selectedYear.value = year
    return fetchYear(propertyId)
  }

  /** Loads every pending or overdue payment of the account, recording whether the request failed; ignores the result if a newer fetch or an account change already happened. */
  async function fetchAlerts() {
    const requestToken = ++latestAlertsToken
    try {
      const fetched = await paymentService.fetchPaymentAlerts()
      if (requestToken !== latestAlertsToken) return
      alerts.value = fetched
      hasAlertsLoadError.value = false
    } catch {
      if (requestToken !== latestAlertsToken) return
      hasAlertsLoadError.value = true
    }
  }

  /** Sends a bank statement to the backend and, on success, opens the preview of the payments found; records why it failed otherwise. */
  async function scanStatement(propertyId: string, file: File): Promise<boolean> {
    const requestEpoch = sessionEpoch
    isScanning.value = true
    scanError.value = null
    try {
      const payments = await paymentService.scanStatement(propertyId, file)
      if (requestEpoch !== sessionEpoch) return false
      scanSession.value = { propertyId, payments }
      return true
    } catch (error) {
      if (requestEpoch !== sessionEpoch) return false
      scanError.value = toRequestError(error)
      return false
    } finally {
      if (requestEpoch === sessionEpoch) isScanning.value = false
    }
  }

  /** Saves the confirmed payments, closes the preview and reloads the property's table and the alerts; returns null and records why if saving failed. */
  async function savePayments(propertyId: string, payments: PaymentToSave[]): Promise<SavePaymentsResult | null> {
    const requestEpoch = sessionEpoch
    isSaving.value = true
    saveError.value = null
    let result: SavePaymentsResult
    try {
      result = await paymentService.savePayments(propertyId, payments)
    } catch (error) {
      if (requestEpoch === sessionEpoch) {
        saveError.value = toRequestError(error)
        isSaving.value = false
      }
      return null
    }
    if (requestEpoch !== sessionEpoch) return null
    isSaving.value = false
    scanSession.value = null
    await Promise.all([fetchYear(propertyId), fetchAlerts()])
    return result
  }

  /** Activates the rent payment check of the property and reloads its table and the alerts; returns false and records why if activating failed. */
  async function activateTracking(propertyId: string): Promise<boolean> {
    const requestEpoch = sessionEpoch
    isActivating.value = true
    activationError.value = null
    try {
      await paymentService.activateTracking(propertyId)
    } catch (error) {
      if (requestEpoch === sessionEpoch) {
        activationError.value = toRequestError(error)
        isActivating.value = false
      }
      return false
    }
    if (requestEpoch !== sessionEpoch) return false
    isActivating.value = false
    await Promise.all([fetchYear(propertyId), fetchAlerts()])
    return true
  }

  /** Clears the failure of the last activation, e.g. once the user tries again. */
  function clearActivationError() {
    activationError.value = null
  }

  /** Discards the scanned payments without saving anything. */
  function discardScan() {
    scanSession.value = null
    saveError.value = null
  }

  /** Clears the failure of the last scan, e.g. once the user starts another one. */
  function clearScanError() {
    scanError.value = null
  }

  /** Forgets every table, alert and scan state, and ignores any still in-flight request, e.g. when the logged-in account changes. */
  function reset() {
    sessionEpoch++
    latestYearToken++
    latestAlertsToken++
    selectedYear.value = new Date().getFullYear()
    overviewsByProperty.value = {}
    hasYearLoadError.value = false
    isYearLoading.value = false
    alerts.value = []
    hasAlertsLoadError.value = false
    isScanning.value = false
    scanError.value = null
    scanSession.value = null
    isSaving.value = false
    saveError.value = null
    isActivating.value = false
    activationError.value = null
  }

  return {
    selectedYear,
    overviewsByProperty,
    hasYearLoadError,
    isYearLoading,
    alerts,
    alertByTenant,
    hasAlertsLoadError,
    isScanning,
    scanError,
    scanSession,
    isPreviewOpen,
    isSaving,
    saveError,
    isActivating,
    activationError,
    alertForProperty,
    fetchYear,
    changeYear,
    fetchAlerts,
    scanStatement,
    savePayments,
    activateTracking,
    clearActivationError,
    discardScan,
    clearScanError,
    reset,
  }
})

/** Extracts the HTTP status and the backend's localized message from a failed request. */
function toRequestError(error: unknown): PaymentRequestError {
  return {
    status: axios.isAxiosError(error) ? (error.response?.status ?? null) : null,
    message: getServerErrorMessage(error),
  }
}
