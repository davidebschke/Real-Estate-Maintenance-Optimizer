import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import PropertyDetailDrawer from '@/components/properties/PropertyDetailDrawer.vue'
import { usePropertiesStore } from '@/stores/properties'
import { useTenantsStore } from '@/stores/tenants'
import * as apartmentService from '@/services/apartmentService'
import * as propertyService from '@/services/propertyService'
import type { Apartment } from '@/types/apartment'
import type { Property } from '@/types/property'

vi.mock('@/services/apartmentService')
vi.mock('@/services/propertyService')
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
  })
})
