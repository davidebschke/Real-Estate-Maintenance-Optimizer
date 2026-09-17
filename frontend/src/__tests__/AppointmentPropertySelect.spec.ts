import { beforeEach, describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import AppointmentPropertySelect from '@/components/appointments/AppointmentPropertySelect.vue'
import * as propertyService from '@/services/propertyService'

vi.mock('@/services/propertyService')

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(propertyService.fetchProperties).mockResolvedValue([
    {
      id: '1',
      name: 'Wohnanlage Sonnenhof',
      address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
      icon: 'pi-building',
      latitude: 50.94,
      longitude: 6.88,
    },
  ])
})

describe('AppointmentPropertySelect', () => {
  it('shows no address caption when nothing is selected', async () => {
    const wrapper = mount(AppointmentPropertySelect, {
      props: { modelValue: null },
      global: { plugins: [i18n, PrimeVue] },
    })
    await flushPromises()

    expect(wrapper.find('.appointment-property-select__address').exists()).toBe(false)
  })

  it("shows the selected property's address as a caption", async () => {
    const wrapper = mount(AppointmentPropertySelect, {
      props: { modelValue: '1' },
      global: { plugins: [i18n, PrimeVue] },
    })
    await flushPromises()

    expect(wrapper.find('.appointment-property-select__address').text()).toContain(
      'Aachener Str. 512',
    )
  })
})
