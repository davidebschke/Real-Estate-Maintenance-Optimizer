import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import PropertyDetailDrawer from '@/components/properties/PropertyDetailDrawer.vue'
import { usePropertiesStore } from '@/stores/properties'
import { useTenantsStore } from '@/stores/tenants'
import { usePaymentsStore } from '@/stores/payments'
import * as apartmentService from '@/services/apartmentService'
import * as propertyService from '@/services/propertyService'
import * as paymentService from '@/services/paymentService'
import {
  createAlertEntry,
  createFormerTenantPayment,
  createMonths,
  createPaymentYearOverview,
  createTenantPaymentYear,
} from '@/__tests__/paymentFixtures'
import type { Apartment } from '@/types/apartment'
import type { Property } from '@/types/property'

vi.mock('@/services/apartmentService')
vi.mock('@/services/propertyService')
vi.mock('@/services/paymentService')
vi.mock('primevue/useconfirm')

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: 'property-1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
    latitude: 50.94,
    longitude: 6.88,
    tenantCount: 0,
    ...overrides,
  }
}

/** Builds a sample apartment with one tenant, with overridable fields. */
function createApartment(overrides: Partial<Apartment> = {}): Apartment {
  return {
    id: 'apartment-1',
    propertyId: 'property-1',
    floor: 2,
    areaSquareMeters: 64.5,
    totalRent: 850,
    coldRent: 650,
    additionalCosts: 200,
    tenants: [{ id: 'tenant-1', firstName: 'Erika', lastName: 'Mustermann' }],
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  vi.mocked(apartmentService.fetchApartments).mockReset().mockResolvedValue([])
  vi.mocked(propertyService.fetchProperties).mockReset().mockResolvedValue([])
  vi.mocked(apartmentService.deleteTenant).mockReset().mockResolvedValue()
  vi.mocked(paymentService.fetchPaymentYear).mockReset().mockResolvedValue(createPaymentYearOverview())
  vi.mocked(paymentService.fetchPaymentAlerts).mockReset().mockResolvedValue([])
  vi.mocked(paymentService.activateTracking).mockReset().mockResolvedValue({ trackingStart: '2026-01' })
  vi.mocked(useConfirm).mockReturnValue({ require: vi.fn() } as never)
  document.body.innerHTML = ''
  usePropertiesStore().properties = [createProperty()]
})

/** Mounts the drawer already open for the given property; PrimeVue's Drawer teleports its content one microtask later. */
async function mountDrawer(propertyId: string | null = 'property-1') {
  const wrapper = mount(PropertyDetailDrawer, {
    props: { visible: true, propertyId },
    global: { plugins: [i18n, PrimeVue] },
    attachTo: document.body,
  })
  await flushPromises()
  return wrapper
}

describe('PropertyDetailDrawer', () => {
  it('shows the name and address of the property and loads its apartments', async () => {
    await mountDrawer()

    expect(document.body.querySelector('.property-detail-drawer__name')?.textContent).toBe('Wohnanlage Sonnenhof')
    expect(document.body.querySelector('.property-detail-drawer__address')?.textContent).toBe(
      'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    )
    expect(apartmentService.fetchApartments).toHaveBeenCalledWith('property-1')
  })

  it('lists every apartment of the property with its tenants', async () => {
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([
      createApartment(),
      createApartment({
        id: 'apartment-2',
        floor: 0,
        tenants: [{ id: 'tenant-2', firstName: 'Max', lastName: 'Meier' }],
      }),
    ])

    await mountDrawer()

    const floors = Array.from(document.body.querySelectorAll('.apartment-card__floor')).map((floor) => floor.textContent)
    expect(floors).toEqual(['2. Stockwerk', 'Erdgeschoss'])
    const names = Array.from(document.body.querySelectorAll('.apartment-card__tenant-name')).map((name) => name.textContent)
    expect(names).toEqual(['Erika Mustermann', 'Max Meier'])
  })

  it('shows a hint when the property has no tenants yet', async () => {
    await mountDrawer()

    expect(document.body.querySelector('.property-detail-drawer__empty')?.textContent).toBe(
      'Für dieses Objekt sind noch keine Mieter hinterlegt.',
    )
  })

  it('shows an error instead of the empty hint when loading fails', async () => {
    vi.mocked(apartmentService.fetchApartments).mockRejectedValue(new Error('Network Error'))

    await mountDrawer()

    expect(document.body.querySelector('.property-detail-drawer__error')?.textContent).toBe(
      'Die Mieter konnten nicht geladen werden.',
    )
    expect(document.body.querySelector('.property-detail-drawer__empty')).toBeNull()
  })

  it('opens the tenant form for a new apartment of this property', async () => {
    await mountDrawer()

    ;(document.body.querySelector('.property-detail-drawer__create') as HTMLElement).click()

    expect(useTenantsStore().formTarget).toEqual({ mode: 'create', propertyId: 'property-1' })
  })

  it('opens the tenant form to add a further tenant to an apartment', async () => {
    const apartment = createApartment()
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([apartment])
    await mountDrawer()

    ;(document.body.querySelector('.apartment-card__add-tenant') as HTMLElement).click()

    expect(useTenantsStore().formTarget).toEqual({ mode: 'add', apartment })
  })

  it('opens the tenant form to edit a tenant', async () => {
    const apartment = createApartment()
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([apartment])
    await mountDrawer()

    ;(document.body.querySelector('.apartment-card__tenant-edit') as HTMLElement).click()

    expect(useTenantsStore().formTarget).toEqual({ mode: 'edit', apartment, tenant: apartment.tenants[0] })
  })

  it('deletes a tenant only after the confirmation and warns when it is the last one', async () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([createApartment()])
    await mountDrawer()

    ;(document.body.querySelector('.apartment-card__tenant-delete') as HTMLElement).click()

    expect(apartmentService.deleteTenant).not.toHaveBeenCalled()
    expect(require.mock.calls[0]![0].message).toContain('werden auch die Wohnungsdaten gelöscht')

    ;(require.mock.calls[0]![0].accept as () => void)()
    await flushPromises()

    expect(apartmentService.deleteTenant).toHaveBeenCalledWith('tenant-1')
  })

  it('does not warn about the apartment when other tenants remain', async () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([
      createApartment({
        tenants: [
          { id: 'tenant-1', firstName: 'Erika', lastName: 'Mustermann' },
          { id: 'tenant-2', firstName: 'Max', lastName: 'Mustermann' },
        ],
      }),
    ])
    await mountDrawer()

    ;(document.body.querySelector('.apartment-card__tenant-delete') as HTMLElement).click()

    expect(require.mock.calls[0]![0].message).not.toContain('Wohnungsdaten')
  })

  it('shows an error when deleting a tenant fails', async () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([createApartment()])
    vi.mocked(apartmentService.deleteTenant).mockRejectedValue(new Error('500'))
    await mountDrawer()

    ;(document.body.querySelector('.apartment-card__tenant-delete') as HTMLElement).click()
    ;(require.mock.calls[0]![0].accept as () => void)()
    await flushPromises()

    expect(document.body.querySelector('.property-detail-drawer__error')?.textContent).toBe(
      'Der Mieter konnte nicht gelöscht werden.',
    )
  })

  it('closes when the back button is clicked', async () => {
    const wrapper = await mountDrawer()

    ;(document.body.querySelector('.property-detail-drawer__back') as HTMLElement).click()
    await flushPromises()

    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
  })

  it('renders nothing and loads nothing without a property', async () => {
    await mountDrawer(null)

    expect(document.body.querySelector('.property-detail-drawer__name')).toBeNull()
    expect(apartmentService.fetchApartments).not.toHaveBeenCalled()
    expect(paymentService.fetchPaymentYear).not.toHaveBeenCalled()
  })

  describe('rent payments', () => {
    it('loads the payment table of the selected year and shows it below the apartments', async () => {
      await mountDrawer()

      expect(paymentService.fetchPaymentYear).toHaveBeenCalledWith('property-1', usePaymentsStore().selectedYear)
      const headings = Array.from(document.body.querySelectorAll('.property-detail-drawer__section h3')).map((heading) => heading.textContent)
      expect(headings).toEqual(['Mieter', 'Mietzahlungen'])
      expect(document.body.querySelector('.tenant-payment-table__tenant-name')?.textContent).toBe('Erika Mustermann')
      expect(document.body.querySelectorAll('.tenant-payment-table__cell')).toHaveLength(12)
    })

    it('offers the bank statement upload for this property', async () => {
      await mountDrawer()

      expect(document.body.querySelector('.payment-upload-button input[type="file"]')).not.toBeNull()
      expect(document.body.querySelector('.payment-upload-button__button')?.textContent).toContain('Kontoauszug einlesen')
    })

    it('scans a chosen statement for this property and opens the preview in the store', async () => {
      vi.mocked(paymentService.scanStatement).mockResolvedValue([])
      await mountDrawer()
      const input = document.body.querySelector('.payment-upload-button input[type="file"]') as HTMLInputElement
      const file = new File(['%PDF-1.7'], 'statement.pdf', { type: 'application/pdf' })
      Object.defineProperty(input, 'files', { value: [file], configurable: true })

      input.dispatchEvent(new Event('change'))
      await flushPromises()

      expect(paymentService.scanStatement).toHaveBeenCalledWith('property-1', file)
      expect(usePaymentsStore().scanSession).toEqual({ propertyId: 'property-1', payments: [] })
    })

    it('shows the account-wide payment alert next to the affected tenant of an apartment', async () => {
      vi.mocked(apartmentService.fetchApartments).mockResolvedValue([createApartment()])
      usePaymentsStore().alerts = [createAlertEntry({ tenantId: 'tenant-1', alert: 'PENDING' })]

      await mountDrawer()

      expect(document.body.querySelector('.apartment-card__tenant .payment-alert-badge')?.textContent).toContain('Zahlung offen')
    })

    it('highlights a tenant with an alert in the payment table', async () => {
      vi.mocked(paymentService.fetchPaymentYear).mockResolvedValue(
        createPaymentYearOverview({
          tenants: [createTenantPaymentYear({ alert: 'OVERDUE', months: createMonths('OVERDUE') })],
        }),
      )

      await mountDrawer()

      expect(document.body.querySelector('.tenant-payment-table__row--alert')).not.toBeNull()
    })

    it('loads the table of the previous and the next year on request', async () => {
      await mountDrawer()
      const year = usePaymentsStore().selectedYear

      ;(document.body.querySelector('.tenant-payment-table__previous') as HTMLElement).click()
      await flushPromises()
      expect(paymentService.fetchPaymentYear).toHaveBeenLastCalledWith('property-1', year - 1)
      expect(document.body.querySelector('.tenant-payment-table__year')?.textContent).toBe(String(year - 1))

      ;(document.body.querySelector('.tenant-payment-table__next') as HTMLElement).click()
      await flushPromises()
      expect(paymentService.fetchPaymentYear).toHaveBeenLastCalledWith('property-1', year)
    })

    it('does not show the rows of another year under the selected year when its table cannot be loaded', async () => {
      await mountDrawer()
      const year = usePaymentsStore().selectedYear
      vi.mocked(paymentService.fetchPaymentYear).mockRejectedValue(new Error('Network Error'))

      ;(document.body.querySelector('.tenant-payment-table__previous') as HTMLElement).click()
      await flushPromises()

      expect(document.body.querySelector('.tenant-payment-table__year')?.textContent).toBe(String(year - 1))
      expect(document.body.querySelector('.tenant-payment-table__tenant-name')).toBeNull()
    })

    it('shows an error hint when the payment table cannot be loaded', async () => {
      vi.mocked(paymentService.fetchPaymentYear).mockRejectedValue(new Error('Network Error'))

      await mountDrawer()

      const errors = Array.from(document.body.querySelectorAll('.property-detail-drawer__error')).map((error) => error.textContent)
      expect(errors).toEqual(['Die Mietzahlungen konnten nicht geladen werden.'])
    })

    it('shows the activation note, the upload and the table once the check is active', async () => {
      await mountDrawer()

      expect(document.body.querySelector('.rent-tracking-activation__active')?.textContent).toContain('Aktiv seit 01/2026')
      expect(document.body.querySelector('.rent-tracking-activation__button')).toBeNull()
      expect(document.body.querySelector('.payment-upload-button')).not.toBeNull()
      expect(document.body.querySelector('.tenant-payment-table')).not.toBeNull()
    })

    it('offers only the activation, without upload and table, while the check is not activated', async () => {
      vi.mocked(paymentService.fetchPaymentYear).mockResolvedValue(createPaymentYearOverview({ trackingStart: null, tenants: [] }))

      await mountDrawer()

      expect(document.body.querySelector('.rent-tracking-activation__explanation')).not.toBeNull()
      expect(document.body.querySelector('.rent-tracking-activation__button')?.textContent).toContain('Mietzahlungscheck aktivieren')
      expect(document.body.querySelector('.payment-upload-button')).toBeNull()
      expect(document.body.querySelector('.tenant-payment-table')).toBeNull()
    })

    it('activates the check for this property and then shows the upload and the table', async () => {
      vi.mocked(paymentService.fetchPaymentYear).mockResolvedValueOnce(createPaymentYearOverview({ trackingStart: null, tenants: [] }))
      await mountDrawer()

      ;(document.body.querySelector('.rent-tracking-activation__button') as HTMLElement).click()
      await flushPromises()

      expect(paymentService.activateTracking).toHaveBeenCalledWith('property-1')
      expect(document.body.querySelector('.rent-tracking-activation__active')?.textContent).toContain('Aktiv seit 01/2026')
      expect(document.body.querySelector('.payment-upload-button')).not.toBeNull()
      expect(document.body.querySelector('.tenant-payment-table')).not.toBeNull()
    })

    it('lists the payments of former tenants below the table only when there are some', async () => {
      await mountDrawer()
      expect(document.body.querySelector('.former-tenant-payment-list')).toBeNull()
      document.body.innerHTML = ''

      vi.mocked(paymentService.fetchPaymentYear).mockResolvedValue(
        createPaymentYearOverview({ formerTenantPayments: [createFormerTenantPayment()] }),
      )
      await mountDrawer()

      expect(document.body.querySelector('.former-tenant-payment-list__heading')?.textContent).toBe('Zahlungen früherer Mieter')
      expect(document.body.querySelector('.former-tenant-payment-list__note')?.textContent).toContain(
        'Zahlung des vorherigen Mieters Mustermann, Erika',
      )
    })

    it('does not show former tenant payments of another year under the selected year', async () => {
      vi.mocked(paymentService.fetchPaymentYear).mockResolvedValue(
        createPaymentYearOverview({ formerTenantPayments: [createFormerTenantPayment()] }),
      )
      await mountDrawer()
      vi.mocked(paymentService.fetchPaymentYear).mockRejectedValue(new Error('Network Error'))

      ;(document.body.querySelector('.tenant-payment-table__previous') as HTMLElement).click()
      await flushPromises()

      expect(document.body.querySelector('.former-tenant-payment-list')).toBeNull()
    })

    it('reloads the table and the alerts after a tenant was deleted', async () => {
      const require = vi.fn<(options: Record<string, unknown>) => void>()
      vi.mocked(useConfirm).mockReturnValue({ require } as never)
      vi.mocked(apartmentService.fetchApartments).mockResolvedValue([createApartment()])
      await mountDrawer()
      vi.mocked(paymentService.fetchPaymentYear).mockClear()
      vi.mocked(paymentService.fetchPaymentAlerts).mockClear()

      ;(document.body.querySelector('.apartment-card__tenant-delete') as HTMLElement).click()
      ;(require.mock.calls[0]![0].accept as () => void)()
      await flushPromises()

      expect(paymentService.fetchPaymentYear).toHaveBeenCalledWith('property-1', usePaymentsStore().selectedYear)
      expect(paymentService.fetchPaymentAlerts).toHaveBeenCalledTimes(1)
    })

    it('does not reload the rent payments when deleting a tenant fails', async () => {
      const require = vi.fn<(options: Record<string, unknown>) => void>()
      vi.mocked(useConfirm).mockReturnValue({ require } as never)
      vi.mocked(apartmentService.fetchApartments).mockResolvedValue([createApartment()])
      vi.mocked(apartmentService.deleteTenant).mockRejectedValue(new Error('500'))
      await mountDrawer()
      vi.mocked(paymentService.fetchPaymentYear).mockClear()

      ;(document.body.querySelector('.apartment-card__tenant-delete') as HTMLElement).click()
      ;(require.mock.calls[0]![0].accept as () => void)()
      await flushPromises()

      expect(paymentService.fetchPaymentYear).not.toHaveBeenCalled()
    })

    it('shows the empty hint of the table for a property without tenants', async () => {
      vi.mocked(paymentService.fetchPaymentYear).mockResolvedValue(createPaymentYearOverview({ tenants: [] }))

      await mountDrawer()

      expect(document.body.querySelector('.tenant-payment-table__empty')).not.toBeNull()
    })
  })
})
