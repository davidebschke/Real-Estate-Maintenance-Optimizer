import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import PropertyList from '@/components/properties/PropertyList.vue'
import { usePropertiesStore } from '@/stores/properties'
import * as propertyService from '@/services/propertyService'
import * as appointmentService from '@/services/appointmentService'
import * as paymentService from '@/services/paymentService'
import { createAlertEntry } from '@/__tests__/paymentFixtures'
import type { Property } from '@/types/property'

vi.mock('@/services/propertyService')
vi.mock('@/services/appointmentService')
vi.mock('@/services/paymentService')
vi.mock('primevue/useconfirm')

const globalMountOptions = { plugins: [PrimeVue, i18n], directives: { tooltip: {} } }

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: '1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
    latitude: 50.94,
    longitude: 6.88,
    tenantCount: 0,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 7, 10, 9, 0))
  vi.mocked(propertyService.fetchProperties).mockReset()
  vi.mocked(appointmentService.fetchAppointments).mockReset().mockResolvedValue([])
  vi.mocked(paymentService.fetchPaymentAlerts).mockReset().mockResolvedValue([])
  vi.mocked(useConfirm).mockReturnValue({
    require: (options: { accept?: () => void }) => options.accept?.(),
  } as never)
})

afterEach(() => {
  vi.useRealTimers()
})

describe('PropertyList', () => {
  it('shows an empty-state message when there are no properties', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([])
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()

    expect(wrapper.find('.property-list__empty').exists()).toBe(true)
  })

  it('shows an error message when loading properties fails', async () => {
    vi.mocked(propertyService.fetchProperties).mockRejectedValue(new Error('network error'))
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()

    expect(wrapper.find('.property-list__error').exists()).toBe(true)
    expect(wrapper.find('.property-list__empty').exists()).toBe(false)
  })

  it('renders one card per property with its tenant count', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([
      createProperty({ id: '1', name: 'Wohnanlage Sonnenhof', tenantCount: 4 }),
      createProperty({ id: '2', name: 'Wohnpark Lindenthal', tenantCount: 0 }),
    ])

    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()

    const cards = wrapper.findAll('.property-card')
    expect(cards).toHaveLength(2)
    expect(cards[0]!.find('.property-card__name').text()).toBe('Wohnanlage Sonnenhof')
    expect(cards[0]!.find('.property-card__stat-value').text()).toBe('4')
    expect(cards[1]!.find('.property-card__stat-value').text()).toBe('0')
  })

  it('loads the payment alerts of the account along with the properties', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([])
    mount(PropertyList, { global: globalMountOptions })
    await flushPromises()

    expect(paymentService.fetchPaymentAlerts).toHaveBeenCalledTimes(1)
  })

  it('marks every property with the worst payment alert of its tenants', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([
      createProperty({ id: '1', name: 'Wohnanlage Sonnenhof' }),
      createProperty({ id: '2', name: 'Wohnpark Lindenthal' }),
      createProperty({ id: '3', name: 'Rheinhaus Deutz' }),
    ])
    vi.mocked(paymentService.fetchPaymentAlerts).mockResolvedValue([
      createAlertEntry({ propertyId: '1', tenantId: 'tenant-1', alert: 'PENDING' }),
      createAlertEntry({ propertyId: '1', tenantId: 'tenant-2', alert: 'OVERDUE' }),
      createAlertEntry({ propertyId: '2', tenantId: 'tenant-3', alert: 'PENDING' }),
    ])
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()

    const alerts = wrapper.findAll('.property-card').map((card) => {
      const badge = card.find('.property-card__payment-alert')
      return badge.exists() ? badge.text() : null
    })
    expect(alerts).toEqual(['Zahlung überfällig', 'Zahlung offen', null])
  })

  it('shows a hint when the payment alerts cannot be loaded but still lists the properties', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([createProperty()])
    vi.mocked(paymentService.fetchPaymentAlerts).mockRejectedValue(new Error('network error'))
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()

    expect(wrapper.find('.property-list__error').text()).toBe('Offene Mietzahlungen konnten nicht geladen werden.')
    expect(wrapper.findAll('.property-card')).toHaveLength(1)
  })

  it('still shows the empty-state message next to an alerts error when there are no properties', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([])
    vi.mocked(paymentService.fetchPaymentAlerts).mockRejectedValue(new Error('network error'))
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()

    expect(wrapper.find('.property-list__empty').exists()).toBe(true)
  })

  it('opens the property creation dialog when the pinned create card is clicked', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([])
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()
    const propertiesStore = usePropertiesStore()

    await wrapper.find('.property-create-card').trigger('click')

    expect(propertiesStore.isCreateDialogOpen).toBe(true)
  })

  it('opens the property detail view when a card name is clicked', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([createProperty({ id: '1' })])
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()
    const propertiesStore = usePropertiesStore()

    await wrapper.find('.property-card__details').trigger('click')

    expect(propertiesStore.activeDetailPropertyId).toBe('1')
  })

  it('opens the property edit dialog with the clicked property', async () => {
    const property = createProperty({ id: '1' })
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([property])
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()
    const propertiesStore = usePropertiesStore()

    await wrapper.find('.property-card-controls__edit').trigger('click')

    expect(propertiesStore.editingProperty).toEqual(property)
  })

  it('deletes a property after confirmation and refreshes the appointments', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([createProperty({ id: '1' })])
    vi.mocked(propertyService.deleteProperty).mockResolvedValue(undefined)
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()
    vi.mocked(appointmentService.fetchAppointments).mockClear()

    await wrapper.find('.property-card-controls__delete').trigger('click')
    await flushPromises()

    expect(propertyService.deleteProperty).toHaveBeenCalledWith('1')
    expect(appointmentService.fetchAppointments).toHaveBeenCalled()
    expect(wrapper.findAll('.property-card')).toHaveLength(0)
  })

  it('shows an error message when deleting a property fails', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([createProperty({ id: '1' })])
    vi.mocked(propertyService.deleteProperty).mockRejectedValue(new Error('network error'))
    const wrapper = mount(PropertyList, { global: globalMountOptions })
    await flushPromises()

    await wrapper.find('.property-card-controls__delete').trigger('click')
    await flushPromises()

    expect(wrapper.find('.property-list__error').exists()).toBe(true)
  })
})
