import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import ApartmentCard from '@/components/tenants/ApartmentCard.vue'
import type { Apartment } from '@/types/apartment'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Builds a sample apartment with two tenants, with overridable fields. */
function createApartment(overrides: Partial<Apartment> = {}): Apartment {
  return {
    id: 'apartment-1',
    propertyId: 'property-1',
    floor: 2,
    areaSquareMeters: 64.5,
    totalRent: 850,
    coldRent: 650,
    additionalCosts: 200,
    tenants: [
      { id: 'tenant-1', firstName: 'Erika', lastName: 'Mustermann' },
      { id: 'tenant-2', firstName: 'Max', lastName: 'Mustermann' },
    ],
    ...overrides,
  }
}

/** Mounts the card for the given apartment. */
function mountCard(apartment: Apartment) {
  return mount(ApartmentCard, { props: { apartment }, global: { plugins: [i18n, PrimeVue] } })
}

describe('ApartmentCard', () => {
  it('shows the floor, the area and the three rent figures of the apartment', () => {
    const wrapper = mountCard(createApartment())

    expect(wrapper.find('.apartment-card__floor').text()).toBe('2. Stockwerk')
    expect(wrapper.find('.apartment-card__area').text()).toBe('64,5 m²')
    const figures = wrapper.findAll('.apartment-card__figure').map((figure) => figure.text())
    expect(figures[0]).toMatch(/^Mietpreis\s*850,00\s€$/)
    expect(figures[1]).toMatch(/^Kaltmiete\s*650,00\s€$/)
    expect(figures[2]).toMatch(/^Nebenkosten\s*200,00\s€$/)
  })

  it('lists every tenant of the apartment by full name', () => {
    const wrapper = mountCard(createApartment())

    expect(wrapper.findAll('.apartment-card__tenant-name').map((name) => name.text())).toEqual([
      'Erika Mustermann',
      'Max Mustermann',
    ])
  })

  it('shows a payment alert badge only for the tenants that have an alert', () => {
    const wrapper = mount(ApartmentCard, {
      props: { apartment: createApartment(), alertByTenant: { 'tenant-2': 'OVERDUE' } },
      global: { plugins: [i18n, PrimeVue] },
    })

    const tenants = wrapper.findAll('.apartment-card__tenant')
    expect(tenants[0]!.find('.payment-alert-badge').exists()).toBe(false)
    expect(tenants[1]!.find('.payment-alert-badge').text()).toBe('Zahlung überfällig')
  })

  it('shows no payment alert badge without alerts', () => {
    const wrapper = mountCard(createApartment())

    expect(wrapper.find('.payment-alert-badge').exists()).toBe(false)
  })

  it('emits the apartment when another tenant is to be added', async () => {
    const apartment = createApartment()
    const wrapper = mountCard(apartment)

    await wrapper.find('.apartment-card__add-tenant').trigger('click')

    expect(wrapper.emitted('add-tenant')).toEqual([[apartment]])
  })

  it('emits the apartment and the tenant when a tenant is to be edited', async () => {
    const apartment = createApartment()
    const wrapper = mountCard(apartment)

    await wrapper.findAll('.apartment-card__tenant-edit')[1]!.trigger('click')

    expect(wrapper.emitted('edit-tenant')).toEqual([[apartment, apartment.tenants[1]]])
  })

  it('emits the apartment and the tenant when a tenant is to be deleted', async () => {
    const apartment = createApartment()
    const wrapper = mountCard(apartment)

    await wrapper.findAll('.apartment-card__tenant-delete')[0]!.trigger('click')

    expect(wrapper.emitted('delete-tenant')).toEqual([[apartment, apartment.tenants[0]]])
  })

  it('names the tenant in the accessible labels of the edit and delete buttons', () => {
    const wrapper = mountCard(createApartment())

    expect(wrapper.find('.apartment-card__tenant-edit').attributes('aria-label')).toBe('Mieter Erika Mustermann bearbeiten')
    expect(wrapper.find('.apartment-card__tenant-delete').attributes('aria-label')).toBe('Mieter Erika Mustermann löschen')
  })

  it('emits the apartment when it is to be edited', async () => {
    const apartment = createApartment()
    const wrapper = mountCard(apartment)

    await wrapper.find('.apartment-card__edit').trigger('click')

    expect(wrapper.emitted('edit-apartment')).toEqual([[apartment]])
  })

  it('emits the apartment when it is to be deleted', async () => {
    const apartment = createApartment()
    const wrapper = mountCard(apartment)

    await wrapper.find('.apartment-card__delete').trigger('click')

    expect(wrapper.emitted('delete-apartment')).toEqual([[apartment]])
  })

  it('names the apartment by its floor in the accessible labels of its edit and delete buttons', () => {
    const wrapper = mountCard(createApartment())

    expect(wrapper.find('.apartment-card__edit').attributes('aria-label')).toBe('Wohnung 2. Stockwerk bearbeiten')
    expect(wrapper.find('.apartment-card__delete').attributes('aria-label')).toBe('Wohnung 2. Stockwerk löschen')
  })

  it('shows a vacancy note instead of the tenant list for an apartment without tenants', () => {
    const wrapper = mountCard(createApartment({ tenants: [] }))

    expect(wrapper.find('.apartment-card__vacancy').text()).toBe('Leerstand – noch kein Mieter')
    expect(wrapper.find('.apartment-card__tenants').exists()).toBe(false)
  })

  it('shows no vacancy note while the apartment has tenants', () => {
    const wrapper = mountCard(createApartment())

    expect(wrapper.find('.apartment-card__vacancy').exists()).toBe(false)
  })

  it('still offers adding a tenant to a vacant apartment', async () => {
    const apartment = createApartment({ tenants: [] })
    const wrapper = mountCard(apartment)

    await wrapper.find('.apartment-card__add-tenant').trigger('click')

    expect(wrapper.emitted('add-tenant')).toEqual([[apartment]])
  })

  it('labels the ground floor and renders English texts when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mountCard(createApartment({ floor: 0, tenants: [] }))

    expect(wrapper.find('.apartment-card__floor').text()).toBe('Ground floor')
    expect(wrapper.find('.apartment-card__add-tenant').text()).toBe('Add tenant')
    expect(wrapper.find('.apartment-card__vacancy').text()).toBe('Vacant – no tenant yet')
    expect(wrapper.find('.apartment-card__edit').attributes('aria-label')).toBe('Edit apartment Ground floor')
  })
})
