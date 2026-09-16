import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import PropertiesView from '@/views/PropertiesView.vue'
import * as propertyService from '@/services/propertyService'
import * as appointmentService from '@/services/appointmentService'

vi.mock('@/services/propertyService')
vi.mock('@/services/appointmentService')

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  vi.mocked(propertyService.fetchProperties).mockResolvedValue([])
  vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('PropertiesView', () => {
  it('renders the localized heading', async () => {
    const wrapper = mount(PropertiesView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()

    expect(wrapper.find('.properties-view__heading').text()).toBe('Objekte')
  })

  it('renders the English heading when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(PropertiesView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()

    expect(wrapper.find('.properties-view__heading').text()).toBe('Properties')
  })
})
