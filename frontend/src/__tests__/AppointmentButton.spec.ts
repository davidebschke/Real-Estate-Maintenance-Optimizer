import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import Button from 'primevue/button'
import { i18n } from '@/i18n'
import AppointmentButton from '@/components/layout/AppointmentButton.vue'

describe('AppointmentButton', () => {
  it('renders a disabled button with the localized label', () => {
    const wrapper = shallowMount(AppointmentButton, {
      global: { plugins: [i18n] },
    })
    const button = wrapper.findComponent(Button)

    // The auto-stub does not replicate Button's prop schema, so passed values surface as
    // plain HTML attributes on the stub element rather than as component props.
    expect(button.attributes('label')).toBe('+ Termin')
    expect(button.attributes('disabled')).toBe('')
  })
})
