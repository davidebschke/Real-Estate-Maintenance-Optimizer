import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { usePaymentsStore } from '@/stores/payments'
import * as paymentService from '@/services/paymentService'
import { httpError } from '@/__tests__/httpError'
import {
  createAlertEntry,
  createPaymentYearOverview,
  createScannedPayment,
} from '@/__tests__/paymentFixtures'
import type { PaymentToSave } from '@/types/payment'

vi.mock('@/services/paymentService')

const statement = new File(['%PDF-1.7'], 'statement.pdf', { type: 'application/pdf' })

const paymentToSave: PaymentToSave = {
  tenantId: 'tenant-1',
  paymentDate: '2026-10-02',
  amount: 950,
  reference: 'Miete Oktober 2026',
  periodYear: 2026,
  periodMonth: 10,
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(paymentService.fetchPaymentYear).mockReset().mockResolvedValue(createPaymentYearOverview())
  vi.mocked(paymentService.fetchPaymentAlerts).mockReset().mockResolvedValue([])
  vi.mocked(paymentService.scanStatement).mockReset().mockResolvedValue([createScannedPayment()])
  vi.mocked(paymentService.savePayments).mockReset().mockResolvedValue({ savedCount: 1, skippedCount: 0 })
  vi.mocked(paymentService.activateTracking).mockReset().mockResolvedValue({ trackingStart: '2026-01' })
})

describe('usePaymentsStore', () => {
  it('starts on the current year without tables, alerts, scan or errors', () => {
    const store = usePaymentsStore()

    expect(store.selectedYear).toBe(new Date().getFullYear())
    expect(store.overviewsByProperty).toEqual({})
    expect(store.alerts).toEqual([])
    expect(store.scanSession).toBeNull()
    expect(store.isPreviewOpen).toBe(false)
    expect(store.scanError).toBeNull()
    expect(store.saveError).toBeNull()
    expect(store.isActivating).toBe(false)
    expect(store.activationError).toBeNull()
    expect(store.hasYearLoadError).toBe(false)
    expect(store.hasAlertsLoadError).toBe(false)
  })

  describe('payment table', () => {
    it('loads the table of a property for the selected year into its own entry', async () => {
      const store = usePaymentsStore()
      store.selectedYear = 2026

      await store.fetchYear('property-1')

      expect(paymentService.fetchPaymentYear).toHaveBeenCalledWith('property-1', 2026)
      expect(store.overviewsByProperty['property-1']).toEqual(createPaymentYearOverview())
      expect(store.hasYearLoadError).toBe(false)
      expect(store.isYearLoading).toBe(false)
    })

    it('shows the loading state while the table is on its way', async () => {
      let resolveFetch!: (overview: ReturnType<typeof createPaymentYearOverview>) => void
      vi.mocked(paymentService.fetchPaymentYear).mockImplementationOnce(
        () => new Promise((resolve) => (resolveFetch = resolve)),
      )
      const store = usePaymentsStore()

      const pending = store.fetchYear('property-1')
      expect(store.isYearLoading).toBe(true)
      resolveFetch(createPaymentYearOverview())
      await pending

      expect(store.isYearLoading).toBe(false)
    })

    it('records a load error and keeps the previous table when loading fails', async () => {
      const store = usePaymentsStore()
      store.overviewsByProperty = { 'property-1': createPaymentYearOverview() }
      vi.mocked(paymentService.fetchPaymentYear).mockRejectedValue(new Error('Network Error'))

      await store.fetchYear('property-1')

      expect(store.hasYearLoadError).toBe(true)
      expect(store.isYearLoading).toBe(false)
      expect(store.overviewsByProperty['property-1']).toEqual(createPaymentYearOverview())
    })

    it('ignores a table that was overtaken by a newer request', async () => {
      let resolveFirst!: (overview: ReturnType<typeof createPaymentYearOverview>) => void
      vi.mocked(paymentService.fetchPaymentYear)
        .mockImplementationOnce(() => new Promise((resolve) => (resolveFirst = resolve)))
        .mockResolvedValueOnce(createPaymentYearOverview({ year: 2025 }))
      const store = usePaymentsStore()

      const first = store.fetchYear('property-1')
      await store.fetchYear('property-1')
      resolveFirst(createPaymentYearOverview({ year: 2024 }))
      await first

      expect(store.overviewsByProperty['property-1']?.year).toBe(2025)
    })

    it('selects another year and loads the table for it', async () => {
      const store = usePaymentsStore()

      await store.changeYear('property-1', 2025)

      expect(store.selectedYear).toBe(2025)
      expect(paymentService.fetchPaymentYear).toHaveBeenCalledWith('property-1', 2025)
    })
  })

  describe('alerts', () => {
    it('loads the account-wide alerts and looks them up by tenant', async () => {
      vi.mocked(paymentService.fetchPaymentAlerts).mockResolvedValue([
        createAlertEntry({ tenantId: 'tenant-1', alert: 'OVERDUE' }),
        createAlertEntry({ tenantId: 'tenant-2', alert: 'PENDING' }),
      ])
      const store = usePaymentsStore()

      await store.fetchAlerts()

      expect(store.alertByTenant).toEqual({ 'tenant-1': 'OVERDUE', 'tenant-2': 'PENDING' })
      expect(store.hasAlertsLoadError).toBe(false)
    })

    it('rates a property by the worst alert of its tenants', async () => {
      vi.mocked(paymentService.fetchPaymentAlerts).mockResolvedValue([
        createAlertEntry({ propertyId: 'property-1', tenantId: 'tenant-1', alert: 'PENDING' }),
        createAlertEntry({ propertyId: 'property-1', tenantId: 'tenant-2', alert: 'OVERDUE' }),
        createAlertEntry({ propertyId: 'property-2', tenantId: 'tenant-3', alert: 'PENDING' }),
      ])
      const store = usePaymentsStore()

      await store.fetchAlerts()

      expect(store.alertForProperty('property-1')).toBe('OVERDUE')
      expect(store.alertForProperty('property-2')).toBe('PENDING')
      expect(store.alertForProperty('property-3')).toBe('NONE')
    })

    it('records a load error and keeps the previous alerts when loading fails', async () => {
      const store = usePaymentsStore()
      store.alerts = [createAlertEntry()]
      vi.mocked(paymentService.fetchPaymentAlerts).mockRejectedValue(new Error('Network Error'))

      await store.fetchAlerts()

      expect(store.hasAlertsLoadError).toBe(true)
      expect(store.alerts).toEqual([createAlertEntry()])
    })

    it('ignores alerts that were overtaken by a newer request', async () => {
      let resolveFirst!: (alerts: ReturnType<typeof createAlertEntry>[]) => void
      vi.mocked(paymentService.fetchPaymentAlerts)
        .mockImplementationOnce(() => new Promise((resolve) => (resolveFirst = resolve)))
        .mockResolvedValueOnce([])
      const store = usePaymentsStore()

      const first = store.fetchAlerts()
      await store.fetchAlerts()
      resolveFirst([createAlertEntry()])
      await first

      expect(store.alerts).toEqual([])
    })
  })

  describe('scanning a statement', () => {
    it('opens the preview with the scanned payments of the property', async () => {
      const store = usePaymentsStore()

      const succeeded = await store.scanStatement('property-1', statement)

      expect(succeeded).toBe(true)
      expect(paymentService.scanStatement).toHaveBeenCalledWith('property-1', statement)
      expect(store.scanSession).toEqual({ propertyId: 'property-1', payments: [createScannedPayment()] })
      expect(store.isPreviewOpen).toBe(true)
      expect(store.isScanning).toBe(false)
    })

    it('opens the preview even when the statement contains no payments', async () => {
      vi.mocked(paymentService.scanStatement).mockResolvedValue([])
      const store = usePaymentsStore()

      await store.scanStatement('property-1', statement)

      expect(store.scanSession).toEqual({ propertyId: 'property-1', payments: [] })
    })

    it('shows the scanning state while the AI reads the statement', async () => {
      let resolveScan!: (payments: ReturnType<typeof createScannedPayment>[]) => void
      vi.mocked(paymentService.scanStatement).mockImplementationOnce(
        () => new Promise((resolve) => (resolveScan = resolve)),
      )
      const store = usePaymentsStore()

      const pending = store.scanStatement('property-1', statement)
      expect(store.isScanning).toBe(true)
      resolveScan([])
      await pending

      expect(store.isScanning).toBe(false)
    })

    it.each([400, 413, 429, 501, 503])('records status %i and the backend message and opens no preview', async (status) => {
      vi.mocked(paymentService.scanStatement).mockRejectedValue(httpError(status, { message: 'Fehler vom Server' }))
      const store = usePaymentsStore()

      const succeeded = await store.scanStatement('property-1', statement)

      expect(succeeded).toBe(false)
      expect(store.scanError).toEqual({ status, message: 'Fehler vom Server' })
      expect(store.scanSession).toBeNull()
      expect(store.isScanning).toBe(false)
    })

    it('records an error without status or message for a failure without a response', async () => {
      vi.mocked(paymentService.scanStatement).mockRejectedValue(new Error('Network Error'))
      const store = usePaymentsStore()

      await store.scanStatement('property-1', statement)

      expect(store.scanError).toEqual({ status: null, message: null })
    })

    it('clears the previous scan error when a new scan starts and on request', async () => {
      vi.mocked(paymentService.scanStatement).mockRejectedValueOnce(httpError(503))
      const store = usePaymentsStore()
      await store.scanStatement('property-1', statement)
      expect(store.scanError).not.toBeNull()

      await store.scanStatement('property-1', statement)
      expect(store.scanError).toBeNull()

      vi.mocked(paymentService.scanStatement).mockRejectedValueOnce(httpError(503))
      await store.scanStatement('property-1', statement)
      store.clearScanError()
      expect(store.scanError).toBeNull()
    })

    it('discards the scanned payments without saving when the preview is closed', async () => {
      const store = usePaymentsStore()
      await store.scanStatement('property-1', statement)

      store.isPreviewOpen = false

      expect(store.scanSession).toBeNull()
      expect(paymentService.savePayments).not.toHaveBeenCalled()
    })
  })

  describe('saving payments', () => {
    it('saves, closes the preview and reloads the table and the alerts', async () => {
      const store = usePaymentsStore()
      await store.scanStatement('property-1', statement)

      const result = await store.savePayments('property-1', [paymentToSave])

      expect(result).toEqual({ savedCount: 1, skippedCount: 0 })
      expect(paymentService.savePayments).toHaveBeenCalledWith('property-1', [paymentToSave])
      expect(store.scanSession).toBeNull()
      expect(store.isSaving).toBe(false)
      expect(paymentService.fetchPaymentYear).toHaveBeenCalledWith('property-1', store.selectedYear)
      expect(paymentService.fetchPaymentAlerts).toHaveBeenCalledTimes(1)
    })

    it('keeps the preview open, records why and reloads nothing when saving fails', async () => {
      vi.mocked(paymentService.savePayments).mockRejectedValue(httpError(404, { message: 'Objekt nicht gefunden.' }))
      const store = usePaymentsStore()
      await store.scanStatement('property-1', statement)

      const result = await store.savePayments('property-1', [paymentToSave])

      expect(result).toBeNull()
      expect(store.saveError).toEqual({ status: 404, message: 'Objekt nicht gefunden.' })
      expect(store.scanSession).not.toBeNull()
      expect(store.isSaving).toBe(false)
      expect(paymentService.fetchPaymentYear).not.toHaveBeenCalled()
      expect(paymentService.fetchPaymentAlerts).not.toHaveBeenCalled()
    })

    it('clears a previous save error when another save starts or the scan is discarded', async () => {
      vi.mocked(paymentService.savePayments).mockRejectedValueOnce(httpError(503))
      const store = usePaymentsStore()
      await store.scanStatement('property-1', statement)
      await store.savePayments('property-1', [paymentToSave])
      expect(store.saveError).not.toBeNull()

      await store.savePayments('property-1', [paymentToSave])
      expect(store.saveError).toBeNull()

      vi.mocked(paymentService.savePayments).mockRejectedValueOnce(httpError(503))
      await store.scanStatement('property-1', statement)
      await store.savePayments('property-1', [paymentToSave])
      store.discardScan()
      expect(store.saveError).toBeNull()
    })
  })

  describe('activating the rent payment check', () => {
    it('activates the check and reloads the table of the property and the alerts', async () => {
      const store = usePaymentsStore()

      const result = await store.activateTracking('property-1')

      expect(result).toBe(true)
      expect(paymentService.activateTracking).toHaveBeenCalledWith('property-1')
      expect(store.isActivating).toBe(false)
      expect(store.activationError).toBeNull()
      expect(paymentService.fetchPaymentYear).toHaveBeenCalledWith('property-1', store.selectedYear)
      expect(paymentService.fetchPaymentAlerts).toHaveBeenCalledTimes(1)
    })

    it('shows the activating state while the request is on its way', async () => {
      let resolveActivation!: (activation: { trackingStart: string }) => void
      vi.mocked(paymentService.activateTracking).mockImplementationOnce(() => new Promise((resolve) => (resolveActivation = resolve)))
      const store = usePaymentsStore()

      const activation = store.activateTracking('property-1')
      expect(store.isActivating).toBe(true)
      resolveActivation({ trackingStart: '2026-01' })
      await activation

      expect(store.isActivating).toBe(false)
    })

    it('records why activating failed and reloads nothing', async () => {
      vi.mocked(paymentService.activateTracking).mockRejectedValue(httpError(404, { message: 'Objekt nicht gefunden.' }))
      const store = usePaymentsStore()

      const result = await store.activateTracking('property-1')

      expect(result).toBe(false)
      expect(store.activationError).toEqual({ status: 404, message: 'Objekt nicht gefunden.' })
      expect(store.isActivating).toBe(false)
      expect(paymentService.fetchPaymentYear).not.toHaveBeenCalled()
      expect(paymentService.fetchPaymentAlerts).not.toHaveBeenCalled()
    })

    it('records an error without status or message for a failure without a response', async () => {
      vi.mocked(paymentService.activateTracking).mockRejectedValue(new Error('Network Error'))
      const store = usePaymentsStore()

      await store.activateTracking('property-1')

      expect(store.activationError).toEqual({ status: null, message: null })
    })

    it('clears the previous activation error when a new activation starts and on request', async () => {
      vi.mocked(paymentService.activateTracking).mockRejectedValueOnce(httpError(503))
      const store = usePaymentsStore()
      await store.activateTracking('property-1')
      expect(store.activationError).not.toBeNull()

      await store.activateTracking('property-1')
      expect(store.activationError).toBeNull()

      vi.mocked(paymentService.activateTracking).mockRejectedValueOnce(httpError(503))
      await store.activateTracking('property-1')
      store.clearActivationError()
      expect(store.activationError).toBeNull()
    })

    it('ignores an activation answered only after the account changed', async () => {
      let resolveActivation!: (activation: { trackingStart: string }) => void
      vi.mocked(paymentService.activateTracking).mockImplementationOnce(() => new Promise((resolve) => (resolveActivation = resolve)))
      const store = usePaymentsStore()

      const activation = store.activateTracking('property-1')
      store.reset()
      resolveActivation({ trackingStart: '2026-01' })

      expect(await activation).toBe(false)
      expect(store.isActivating).toBe(false)
      expect(paymentService.fetchPaymentYear).not.toHaveBeenCalled()
    })

    it('ignores an activation failure answered only after the account changed', async () => {
      let rejectActivation!: (error: unknown) => void
      vi.mocked(paymentService.activateTracking).mockImplementationOnce(() => new Promise((_, reject) => (rejectActivation = reject)))
      const store = usePaymentsStore()

      const activation = store.activateTracking('property-1')
      store.reset()
      rejectActivation(httpError(503))

      expect(await activation).toBe(false)
      expect(store.activationError).toBeNull()
    })
  })

  describe('reset', () => {
    it('forgets every table, alert, scan and error and returns to the current year', async () => {
      vi.mocked(paymentService.fetchPaymentAlerts).mockResolvedValue([createAlertEntry()])
      const store = usePaymentsStore()
      await store.fetchYear('property-1')
      await store.fetchAlerts()
      await store.scanStatement('property-1', statement)
      vi.mocked(paymentService.activateTracking).mockRejectedValueOnce(httpError(409))
      await store.activateTracking('property-1')
      store.selectedYear = 2020

      store.reset()

      expect(store.activationError).toBeNull()
      expect(store.isActivating).toBe(false)
      expect(store.overviewsByProperty).toEqual({})
      expect(store.alerts).toEqual([])
      expect(store.scanSession).toBeNull()
      expect(store.selectedYear).toBe(new Date().getFullYear())
    })

    it('ignores a scan, a save and a table answered only after the account changed', async () => {
      let resolveScan!: (payments: ReturnType<typeof createScannedPayment>[]) => void
      let resolveYear!: (overview: ReturnType<typeof createPaymentYearOverview>) => void
      let resolveAlerts!: (alerts: ReturnType<typeof createAlertEntry>[]) => void
      vi.mocked(paymentService.scanStatement).mockImplementationOnce(() => new Promise((resolve) => (resolveScan = resolve)))
      vi.mocked(paymentService.fetchPaymentYear).mockImplementationOnce(() => new Promise((resolve) => (resolveYear = resolve)))
      vi.mocked(paymentService.fetchPaymentAlerts).mockImplementationOnce(() => new Promise((resolve) => (resolveAlerts = resolve)))
      const store = usePaymentsStore()

      const scan = store.scanStatement('property-1', statement)
      const year = store.fetchYear('property-1')
      const alerts = store.fetchAlerts()
      store.reset()
      resolveScan([createScannedPayment()])
      resolveYear(createPaymentYearOverview())
      resolveAlerts([createAlertEntry()])

      expect(await scan).toBe(false)
      await year
      await alerts
      expect(store.scanSession).toBeNull()
      expect(store.overviewsByProperty).toEqual({})
      expect(store.alerts).toEqual([])
    })

    it('ignores a save answered only after the account changed', async () => {
      let resolveSave!: (result: { savedCount: number; skippedCount: number }) => void
      vi.mocked(paymentService.savePayments).mockImplementationOnce(() => new Promise((resolve) => (resolveSave = resolve)))
      const store = usePaymentsStore()

      const save = store.savePayments('property-1', [paymentToSave])
      store.reset()
      resolveSave({ savedCount: 1, skippedCount: 0 })

      expect(await save).toBeNull()
      expect(paymentService.fetchPaymentYear).not.toHaveBeenCalled()
    })
  })
})
