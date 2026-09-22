import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { i18n } from '@/i18n'
import PropertyCreateCard from '@/components/properties/PropertyCreateCard.vue'
import { usePropertiesStore } from '@/stores/properties'

describe('PropertyCreateCard', () => {
  it('renders a plus icon and label', () => {
    const wrapper = mount(PropertyCreateCard, { global: { plugins: [i18n, createPinia()] } })

    expect(wrapper.find('.property-create-card__icon').classes()).toContain('pi-plus')
    expect(wrapper.find('.property-create-card__label').text()).toBe('Neues Objekt anlegen')
  })

  it('opens the property creation dialog when clicked', async () => {
    const pinia = createPinia()
    const wrapper = mount(PropertyCreateCard, { global: { plugins: [i18n, pinia] } })
    const store = usePropertiesStore()

    await wrapper.trigger('click')

    expect(store.isCreateDialogOpen).toBe(true)
  })
})
