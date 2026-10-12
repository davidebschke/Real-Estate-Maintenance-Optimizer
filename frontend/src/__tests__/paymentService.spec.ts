import { afterEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { fetchPaymentAlerts, fetchPaymentYear, savePayments, scanStatement } from '@/services/paymentService'
import { createAlertEntry, createPaymentYearOverview, createScannedPayment } from '@/__tests__/paymentFixtures'
import type { PaymentToSave } from '@/types/payment'

vi.mock('axios')

afterEach(() => {
  vi.mocked(axios.get).mockReset()
  vi.mocked(axios.post).mockReset()
})

const paymentToSave: PaymentToSave = {
  tenantId: 'tenant-1',
  paymentDate: '2026-10-02',
  amount: 950,
  reference: 'Miete Oktober 2026',
  periodYear: 2026,
  periodMonth: 10,
}

describe('paymentService', () => {
  it('sends the statement as the multipart part "file" and returns the scanned payments', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: { payments: [createScannedPayment()] } })
    const file = new File(['%PDF-1.7'], 'statement.pdf', { type: 'application/pdf' })

    const payments = await scanStatement('property-1', file)

    const [url, body] = vi.mocked(axios.post).mock.calls[0]!
    expect(url).toBe('/api/properties/property-1/rent-payments/scan')
    expect(body).toBeInstanceOf(FormData)
    expect((body as FormData).get('file')).toBeInstanceOf(File)
    expect(((body as FormData).get('file') as File).name).toBe('statement.pdf')
    expect(payments).toEqual([createScannedPayment()])
  })

  it('returns an empty list when the statement contains no payments', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: { payments: [] } })

    expect(await scanStatement('property-1', new File(['x'], 'a.pdf'))).toEqual([])
  })

  it('saves the confirmed payments and returns the counts', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: { savedCount: 1, skippedCount: 0 } })

    const result = await savePayments('property-1', [paymentToSave])

    expect(axios.post).toHaveBeenCalledWith('/api/properties/property-1/rent-payments', { payments: [paymentToSave] })
    expect(result).toEqual({ savedCount: 1, skippedCount: 0 })
  })

  it('fetches the payment table of a property for a year', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: createPaymentYearOverview() })

    const overview = await fetchPaymentYear('property-1', 2026)

    expect(axios.get).toHaveBeenCalledWith('/api/properties/property-1/rent-payments', { params: { year: 2026 } })
    expect(overview).toEqual(createPaymentYearOverview())
  })

  it('fetches the account-wide payment alerts', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [createAlertEntry()] })

    const alerts = await fetchPaymentAlerts()

    expect(axios.get).toHaveBeenCalledWith('/api/rent-payments/alerts')
    expect(alerts).toEqual([createAlertEntry()])
  })

  it('lets a failing request propagate to the caller', async () => {
    vi.mocked(axios.post).mockRejectedValue(new Error('Network Error'))

    await expect(scanStatement('property-1', new File(['x'], 'a.pdf'))).rejects.toThrow('Network Error')
  })
})
