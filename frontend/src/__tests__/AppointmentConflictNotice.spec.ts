import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import AppointmentConflictNotice from '@/components/appointments/AppointmentConflictNotice.vue'
import type { AppointmentConflict } from '@/services/appointmentService'

const conflict: AppointmentConflict = {
  message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
  suggestedStart: new Date(2026, 9, 15, 15, 15),
  suggestedEnd: new Date(2026, 9, 15, 16, 15),
}

describe('AppointmentConflictNotice', () => {
  it('shows the conflict message and the suggested next free slot', () => {
    const wrapper = mount(AppointmentConflictNotice, {
      props: { conflict },
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toContain(conflict.message)
    expect(wrapper.text()).toContain('Nächster freier Zeitpunkt')
    expect(wrapper.text()).toContain('15:15')
  })

  it('emits accept when the user applies the suggestion', async () => {
    const wrapper = mount(AppointmentConflictNotice, {
      props: { conflict },
      global: { plugins: [i18n] },
    })

    await wrapper.get('.appointment-conflict-notice__accept').trigger('click')

    expect(wrapper.emitted('accept')).toHaveLength(1)
  })
})
