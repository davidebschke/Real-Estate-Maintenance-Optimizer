import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import AppointmentPropertySelect from '@/components/appointments/AppointmentPropertySelect.vue'

describe('AppointmentPropertySelect', () => {
  it('shows no address caption when nothing is selected', () => {
    const wrapper = mount(AppointmentPropertySelect, {
      props: { modelValue: null },
      global: { plugins: [i18n, PrimeVue] },
    })

    expect(wrapper.find('.appointment-property-select__address').exists()).toBe(false)
  })

  it("shows the selected property's address as a caption", () => {
    const wrapper = mount(AppointmentPropertySelect, {
      props: { modelValue: '1' },
      global: { plugins: [i18n, PrimeVue] },
    })

    expect(wrapper.find('.appointment-property-select__address').text()).toContain(
      'Aachener Str. 512',
    )
  })
})
