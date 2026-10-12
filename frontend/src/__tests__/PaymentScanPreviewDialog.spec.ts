import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Checkbox from 'primevue/checkbox'
import Select from 'primevue/select'
import { useToast } from 'primevue/usetoast'
import { i18n } from '@/i18n'
import PaymentScanPreviewDialog from '@/components/payments/PaymentScanPreviewDialog.vue'
import { usePaymentsStore } from '@/stores/payments'
import { useTenantsStore } from '@/stores/tenants'
import * as paymentService from '@/services/paymentService'
import { httpError } from '@/__tests__/httpError'
import { createPaymentYearOverview, createScannedPayment } from '@/__tests__/paymentFixtures'
import type { Apartment } from '@/types/apartment'
import type { ScannedPayment } from '@/types/payment'

vi.mock('@/services/paymentService')
vi.mock('primevue/usetoast')

const toastAdd = vi.fn()
const mountedWrappers: { unmount: () => void }[] = []

/** Builds an apartment on the given floor with the given tenants. */
function createApartment(id: string, floor: number, tenants: Apartment['tenants']): Apartment {
  return {
    id,
    propertyId: 'property-1',
    floor,
    areaSquareMeters: 60,
    totalRent: 950,
    coldRent: 750,
    additionalCosts: 200,
    tenants,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  toastAdd.mockReset()
  vi.mocked(useToast).mockReturnValue({ add: toastAdd } as never)
  vi.mocked(paymentService.savePayments).mockReset().mockResolvedValue({ savedCount: 2, skippedCount: 0 })
  vi.mocked(paymentService.fetchPaymentYear).mockReset().mockResolvedValue(createPaymentYearOverview())
  vi.mocked(paymentService.fetchPaymentAlerts).mockReset().mockResolvedValue([])
  document.body.innerHTML = ''
  useTenantsStore().apartmentsByProperty = {
    'property-1': [
      createApartment('apartment-1', 2, [
        { id: 'tenant-1', firstName: 'Erika', lastName: 'Mustermann' },
        { id: 'tenant-2', firstName: 'Max', lastName: 'Mustermann' },
      ]),
      createApartment('apartment-2', 0, [{ id: 'tenant-3', firstName: 'Lisa', lastName: 'Meier' }]),
    ],
  }
})

afterEach(() => {
  mountedWrappers.splice(0).forEach((wrapper) => wrapper.unmount())
})

/** Puts the given payments into the store as the result of a scan of property-1 and mounts the open dialog. */
async function mountDialog(payments: ScannedPayment[]) {
  usePaymentsStore().scanSession = { propertyId: 'property-1', payments }
  const wrapper = mount(PaymentScanPreviewDialog, {
    props: { visible: true },
    global: { plugins: [i18n, PrimeVue] },
    attachTo: document.body,
  })
  mountedWrappers.push(wrapper)
  await flushPromises()
  return wrapper
}

type DialogWrapper = Awaited<ReturnType<typeof mountDialog>>

/** Returns the rows of the dialog's teleported list. */
function rowElements(): HTMLElement[] {
  return Array.from(document.body.querySelectorAll<HTMLElement>('.payment-scan-preview-dialog__row'))
}

/** Returns the tenant Select of the given row. */
function tenantSelect(wrapper: DialogWrapper, rowIndex: number) {
  return wrapper.findAllComponents(Select)[rowIndex * 2]!
}

/** Returns the period Select of the given row. */
function periodSelect(wrapper: DialogWrapper, rowIndex: number) {
  return wrapper.findAllComponents(Select)[rowIndex * 2 + 1]!
}

/** Returns the include Checkbox of the given row. */
function includeCheckbox(wrapper: DialogWrapper, rowIndex: number) {
  return wrapper.findAllComponents(Checkbox)[rowIndex]!
}

/** Returns the save button of the dialog. */
function saveButton(): HTMLButtonElement {
  return document.body.querySelector('.payment-scan-preview-dialog__save') as HTMLButtonElement
}

const confident = createScannedPayment()
const possible = createScannedPayment({
  paymentDate: '2026-10-03',
  amount: 700,
  payerName: 'L. Meier',
  reference: 'Miete 10/2026',
  tenantId: 'tenant-3',
  match: 'POSSIBLE',
})
const unmatched = createScannedPayment({
  paymentDate: '2026-10-05',
  amount: 40,
  payerName: 'Stadtwerke',
  reference: 'Abschlag',
  tenantId: null,
  match: 'NONE',
})
const recorded = createScannedPayment({ paymentDate: '2026-09-02', payerName: 'Erika Mustermann', periodMonth: 9, alreadyRecorded: true })

describe('PaymentScanPreviewDialog', () => {
  it('lists every scanned payment with date, amount, sender and reference', async () => {
    await mountDialog([confident, possible])

    const rows = rowElements()
    expect(rows).toHaveLength(2)
    expect(rows[0]!.querySelector('.payment-scan-preview-dialog__date')!.textContent).toBe('02.10.2026')
    expect(rows[0]!.querySelector('.payment-scan-preview-dialog__amount')!.textContent).toMatch(/^950,00\s€$/)
    expect(rows[0]!.querySelector('.payment-scan-preview-dialog__payer')!.textContent).toBe('Max Mustermann')
    expect(rows[0]!.querySelector('.payment-scan-preview-dialog__reference')!.textContent).toBe('Miete Oktober 2026')
    expect(document.body.querySelector('.payment-scan-preview-dialog__summary')!.textContent!.trim()).toBe(
      'Im Kontoauszug erkannte Zahlungen: 2',
    )
  })

  it('offers the tenants of the property with their floor as options and preselects the suggestion', async () => {
    const wrapper = await mountDialog([confident])

    const select = tenantSelect(wrapper, 0)
    expect(select.props('modelValue')).toBe('tenant-1')
    expect((select.props('options') as { value: string; label: string }[]).map((option) => option.label)).toEqual([
      'Erika Mustermann (2. Stockwerk)',
      'Max Mustermann (2. Stockwerk)',
      'Lisa Meier (Erdgeschoss)',
    ])
  })

  it('preselects the month of the payment and offers a range of months around the current year', async () => {
    const wrapper = await mountDialog([createScannedPayment({ periodYear: 2019, periodMonth: 5 })])

    const select = periodSelect(wrapper, 0)
    expect(select.props('modelValue')).toBe('2019-05')
    const options = select.props('options') as { value: string; label: string }[]
    expect(options.find((option) => option.value === '2019-05')?.label).toBe('Mai 2019')
    expect(options.some((option) => option.value === `${new Date().getFullYear()}-10`)).toBe(true)
  })

  it('selects only confidently matched rows and counts them on the save button', async () => {
    const wrapper = await mountDialog([confident, possible])

    expect(includeCheckbox(wrapper, 0).props('modelValue')).toBe(true)
    expect(includeCheckbox(wrapper, 1).props('modelValue')).toBe(false)
    expect(saveButton().textContent).toContain('1 Zahlung(en) speichern')
  })

  it('deselects a payment that was already recorded and marks it', async () => {
    const wrapper = await mountDialog([confident, recorded])

    expect(includeCheckbox(wrapper, 1).props('modelValue')).toBe(false)
    expect(rowElements()[1]!.querySelector('.payment-scan-preview-dialog__recorded')!.textContent).toContain('Bereits erfasst')
    expect(rowElements()[0]!.querySelector('.payment-scan-preview-dialog__recorded')).toBeNull()
    expect(saveButton().textContent).toContain('1 Zahlung(en) speichern')
  })

  it('leaves a payment without a tenant deselected and its checkbox disabled', async () => {
    const wrapper = await mountDialog([unmatched])

    expect(includeCheckbox(wrapper, 0).props('modelValue')).toBe(false)
    expect(includeCheckbox(wrapper, 0).props('disabled')).toBe(true)
    expect(saveButton().disabled).toBe(true)
  })

  it('shows how confidently each payment was assigned and highlights uncertain and missing ones', async () => {
    await mountDialog([confident, possible, unmatched])

    const rows = rowElements()
    const badges = rows.map((row) => row.querySelector('.payment-scan-preview-dialog__match')!.textContent!.trim())
    expect(badges).toEqual(['Sichere Zuordnung', 'Unsichere Zuordnung', 'Keine Zuordnung'])
    expect(rows[0]!.classList).not.toContain('payment-scan-preview-dialog__row--review')
    expect(rows[1]!.classList).toContain('payment-scan-preview-dialog__row--review')
    expect(rows[2]!.classList).toContain('payment-scan-preview-dialog__row--review')
    expect(document.body.querySelector('.payment-scan-preview-dialog__review-hint')!.textContent).toContain(
      'Bei 2 Zahlung(en)',
    )
  })

  it('selects a payment and ends its highlighting once the user assigns a tenant', async () => {
    const wrapper = await mountDialog([unmatched])

    await tenantSelect(wrapper, 0).vm.$emit('update:modelValue', 'tenant-2')
    await flushPromises()

    expect(includeCheckbox(wrapper, 0).props('modelValue')).toBe(true)
    expect(includeCheckbox(wrapper, 0).props('disabled')).toBe(false)
    expect(rowElements()[0]!.classList).not.toContain('payment-scan-preview-dialog__row--review')
    expect(rowElements()[0]!.querySelector('.payment-scan-preview-dialog__match')!.textContent!.trim()).toBe('Manuell zugeordnet')
    expect(document.body.querySelector('.payment-scan-preview-dialog__review-hint')).toBeNull()
    expect(saveButton().disabled).toBe(false)
  })

  it('saves only the selected payments with their chosen tenant and month', async () => {
    const wrapper = await mountDialog([confident, recorded, unmatched, possible])
    await periodSelect(wrapper, 0).vm.$emit('update:modelValue', '2026-11')
    await tenantSelect(wrapper, 3).vm.$emit('update:modelValue', 'tenant-2')
    await includeCheckbox(wrapper, 2).vm.$emit('update:modelValue', true)
    await flushPromises()

    saveButton().click()
    await flushPromises()

    expect(paymentService.savePayments).toHaveBeenCalledWith('property-1', [
      {
        tenantId: 'tenant-1',
        paymentDate: '2026-10-02',
        amount: 950,
        reference: 'Miete Oktober 2026',
        periodYear: 2026,
        periodMonth: 11,
      },
      {
        tenantId: 'tenant-2',
        paymentDate: '2026-10-03',
        amount: 700,
        reference: 'Miete 10/2026',
        periodYear: 2026,
        periodMonth: 10,
      },
    ])
  })

  it('does not save a payment the user deselected', async () => {
    const wrapper = await mountDialog([confident, possible])
    await includeCheckbox(wrapper, 1).vm.$emit('update:modelValue', true)
    await includeCheckbox(wrapper, 0).vm.$emit('update:modelValue', false)
    await flushPromises()

    saveButton().click()
    await flushPromises()

    const saved = vi.mocked(paymentService.savePayments).mock.calls[0]![1]
    expect(saved.map((payment) => payment.tenantId)).toEqual(['tenant-3'])
  })

  it('confirms the saving in a toast, closes the preview and reloads the table and the alerts', async () => {
    vi.mocked(paymentService.savePayments).mockResolvedValue({ savedCount: 1, skippedCount: 1 })
    await mountDialog([confident])

    saveButton().click()
    await flushPromises()

    expect(toastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Zahlungen gespeichert', detail: 'Gespeichert: 1, übersprungen: 1' }),
    )
    expect(usePaymentsStore().scanSession).toBeNull()
    expect(paymentService.fetchPaymentYear).toHaveBeenCalled()
    expect(paymentService.fetchPaymentAlerts).toHaveBeenCalled()
  })

  it('keeps the preview open and shows the backend message when saving fails', async () => {
    vi.mocked(paymentService.savePayments).mockRejectedValue(httpError(404, { message: 'Objekt nicht gefunden.' }))
    await mountDialog([confident])

    saveButton().click()
    await flushPromises()

    expect(document.body.querySelector('.payment-scan-preview-dialog__error')!.textContent!.trim()).toBe('Objekt nicht gefunden.')
    expect(usePaymentsStore().scanSession).not.toBeNull()
    expect(toastAdd).not.toHaveBeenCalled()
    expect(rowElements()).toHaveLength(1)
  })

  it('shows a generic text when saving fails without a response', async () => {
    vi.mocked(paymentService.savePayments).mockRejectedValue(new Error('Network Error'))
    await mountDialog([confident])

    saveButton().click()
    await flushPromises()

    expect(document.body.querySelector('.payment-scan-preview-dialog__error')!.textContent!.trim()).toBe(
      'Die Zahlungen konnten nicht gespeichert werden.',
    )
  })

  it('shows a hint and offers no saving when the statement contained no payments', async () => {
    await mountDialog([])

    expect(document.body.querySelector('.payment-scan-preview-dialog__empty')!.textContent).toBe(
      'Im Kontoauszug wurden keine Zahlungen erkannt.',
    )
    expect(rowElements()).toHaveLength(0)
    expect(saveButton().disabled).toBe(true)
  })

  it('shows only payments without a tenant as needing review and cannot save any of them', async () => {
    await mountDialog([unmatched, createScannedPayment({ tenantId: null, match: 'NONE', payerName: 'Unbekannt' })])

    expect(document.body.querySelector('.payment-scan-preview-dialog__review-hint')!.textContent).toContain('Bei 2 Zahlung(en)')
    expect(saveButton().disabled).toBe(true)
    await flushPromises()
    expect(paymentService.savePayments).not.toHaveBeenCalled()
  })

  it('closes without saving when cancelled', async () => {
    const wrapper = await mountDialog([confident])

    ;(document.body.querySelector('.payment-scan-preview-dialog__cancel') as HTMLElement).click()
    await flushPromises()

    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
    expect(paymentService.savePayments).not.toHaveBeenCalled()
  })

  it('renders English texts and the English month names when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    const wrapper = await mountDialog([createScannedPayment({ periodYear: 2019, periodMonth: 5 })])

    expect(document.body.querySelector('.payment-scan-preview-dialog__summary')!.textContent!.trim()).toBe(
      'Payments detected in the bank statement: 1',
    )
    expect(saveButton().textContent).toContain('Save 1 payment(s)')
    const options = periodSelect(wrapper, 0).props('options') as { value: string; label: string }[]
    expect(options.find((option) => option.value === '2019-05')?.label).toBe('May 2019')
    expect(document.body.querySelector('.payment-scan-preview-dialog__date')!.textContent).toBe('10/02/2026')
  })
})
