import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import PropertyCard from '@/components/properties/PropertyCard.vue'
import PropertyCardControls from '@/components/properties/PropertyCardControls.vue'
import type { Property } from '@/types/property'

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

afterEach(() => {
  i18n.global.locale.value = 'de'
})

const globalMountOptions = { plugins: [PrimeVue, i18n], directives: { tooltip: {} } }

describe('PropertyCard', () => {
  it('renders the icon, name, address and tenant count', () => {
    const wrapper = mount(PropertyCard, {
      props: { property: createProperty({ tenantCount: 7 }) },
      global: globalMountOptions,
    })

    expect(wrapper.find('.property-card__icon').classes()).toContain('pi-building')
    expect(wrapper.find('.property-card__name').text()).toBe('Wohnanlage Sonnenhof')
    expect(wrapper.find('.property-card__address').text()).toBe(
      'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    )
    expect(wrapper.find('.property-card__stat-value').text()).toBe('7')
    expect(wrapper.find('.property-card__stat-label').text()).toBe('Mieter')
  })

  it('shows a tenant count of zero for a property without tenants', () => {
    const wrapper = mount(PropertyCard, {
      props: { property: createProperty({ tenantCount: 0 }) },
      global: globalMountOptions,
    })

    expect(wrapper.find('.property-card__stat-value').text()).toBe('0')
  })

  it('no longer shows appointment statistics or the next appointment', () => {
    const wrapper = mount(PropertyCard, {
      props: { property: createProperty() },
      global: globalMountOptions,
    })

    expect(wrapper.text()).not.toContain('Offene Aufträge')
    expect(wrapper.text()).not.toContain('Erledigte Aufträge')
    expect(wrapper.text()).not.toContain('Nächster')
    expect(wrapper.text()).not.toContain('anstehender')
  })

  it('renders a very long name and address in full without truncating the content', () => {
    const longName = 'Wohnanlage '.repeat(20).trim()
    const longAddress = 'Sehr lange Musterstraße '.repeat(10).trim()
    const wrapper = mount(PropertyCard, {
      props: { property: createProperty({ name: longName, address: longAddress }) },
      global: globalMountOptions,
    })

    expect(wrapper.find('.property-card__name').text()).toBe(longName)
    expect(wrapper.find('.property-card__address').text()).toBe(longAddress)
  })

  it('emits the property id when the name is clicked to open the details, keeping the name a heading', async () => {
    const wrapper = mount(PropertyCard, {
      props: { property: createProperty({ id: '7' }) },
      global: globalMountOptions,
    })

    expect(wrapper.find('h3').text()).toBe('Wohnanlage Sonnenhof')
    const details = wrapper.find('h3 button.property-card__details')
    expect(details.text()).toBe('Wohnanlage Sonnenhof')

    await details.trigger('click')

    expect(wrapper.emitted('open-detail')).toEqual([['7']])
  })

  it('renders the control panel below the tenant count', () => {
    const wrapper = mount(PropertyCard, {
      props: { property: createProperty() },
      global: globalMountOptions,
    })

    expect(wrapper.findComponent(PropertyCardControls).exists()).toBe(true)
  })

  it('emits the property id when the control panel asks to manage the tenants', async () => {
    const wrapper = mount(PropertyCard, {
      props: { property: createProperty({ id: '7' }) },
      global: globalMountOptions,
    })

    await wrapper.findComponent(PropertyCardControls).vm.$emit('manage-tenants')

    expect(wrapper.emitted('open-detail')).toEqual([['7']])
  })

  it('emits the property when the control panel asks to edit it', async () => {
    const property = createProperty()
    const wrapper = mount(PropertyCard, { props: { property }, global: globalMountOptions })

    await wrapper.findComponent(PropertyCardControls).vm.$emit('edit')

    expect(wrapper.emitted('edit-property')).toEqual([[property]])
  })

  it('emits the property when the control panel asks to delete it', async () => {
    const property = createProperty()
    const wrapper = mount(PropertyCard, { props: { property }, global: globalMountOptions })

    await wrapper.findComponent(PropertyCardControls).vm.$emit('delete')

    expect(wrapper.emitted('delete-property')).toEqual([[property]])
  })
})
