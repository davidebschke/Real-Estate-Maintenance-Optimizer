import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import AppointmentAiSuggestionBanner from '@/components/appointments/AppointmentAiSuggestionBanner.vue'

describe('AppointmentAiSuggestionBanner', () => {
  it('renders the static example suggestion text and action button', () => {
    const wrapper = mount(AppointmentAiSuggestionBanner, {
      global: { plugins: [i18n, PrimeVue] },
    })

    expect(wrapper.text()).toContain('34 km')
    expect(wrapper.text()).toContain('Vorschlag übernehmen')
  })
})
