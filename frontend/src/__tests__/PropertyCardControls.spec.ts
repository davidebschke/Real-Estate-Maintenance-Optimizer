import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import PropertyCardControls from '@/components/properties/PropertyCardControls.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

const globalMountOptions = { plugins: [PrimeVue, i18n] }

describe('PropertyCardControls', () => {
  it('renders a labelled group with the manage tenants, edit and delete buttons', () => {
    const wrapper = mount(PropertyCardControls, { global: globalMountOptions })

    expect(wrapper.find('[role="group"]').attributes('aria-label')).toBe('Objektverwaltung')
    expect(wrapper.find('.property-card-controls__tenants').text()).toBe('Mieter verwalten')
    expect(wrapper.find('.property-card-controls__tenants').attributes('aria-label')).toBe('Mieter verwalten')
    expect(wrapper.find('.property-card-controls__edit').text()).toBe('Bearbeiten')
    expect(wrapper.find('.property-card-controls__delete').text()).toBe('Löschen')
  })

  it('renders the English labels when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(PropertyCardControls, { global: globalMountOptions })

    expect(wrapper.find('.property-card-controls__tenants').text()).toBe('Manage tenants')
    expect(wrapper.find('.property-card-controls__edit').text()).toBe('Edit')
    expect(wrapper.find('.property-card-controls__delete').text()).toBe('Delete')
  })

  it('emits manage-tenants when the tenants button is clicked', async () => {
    const wrapper = mount(PropertyCardControls, { global: globalMountOptions })

    await wrapper.find('.property-card-controls__tenants').trigger('click')

    expect(wrapper.emitted('manage-tenants')).toHaveLength(1)
  })

  it('emits edit when the edit button is clicked', async () => {
    const wrapper = mount(PropertyCardControls, { global: globalMountOptions })

    await wrapper.find('.property-card-controls__edit').trigger('click')

    expect(wrapper.emitted('edit')).toHaveLength(1)
  })

  it('emits delete when the delete button is clicked', async () => {
    const wrapper = mount(PropertyCardControls, { global: globalMountOptions })

    await wrapper.find('.property-card-controls__delete').trigger('click')

    expect(wrapper.emitted('delete')).toHaveLength(1)
  })
})
