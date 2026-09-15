import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import { i18n } from '@/i18n'
import AppointmentButton from '@/components/layout/AppointmentButton.vue'
import { useAppointmentsStore } from '@/stores/appointments'

describe('AppointmentButton', () => {
  it('renders an enabled button with the localized label', () => {
    const wrapper = mount(AppointmentButton, {
      global: { plugins: [i18n, createPinia(), PrimeVue] },
    })
    const button = wrapper.findComponent(Button)

    expect(button.text()).toBe('+ Termin')
    expect(button.attributes('disabled')).toBeUndefined()
  })

  it('opens the appointment creation dialog when clicked', async () => {
    const pinia = createPinia()
    const wrapper = mount(AppointmentButton, {
      global: { plugins: [i18n, pinia, PrimeVue] },
    })
    const store = useAppointmentsStore()

    await wrapper.findComponent(Button).trigger('click')

    expect(store.isCreateDialogOpen).toBe(true)
  })
})
