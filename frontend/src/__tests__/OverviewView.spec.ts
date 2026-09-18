import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import OverviewView from '@/views/OverviewView.vue'
import * as appointmentService from '@/services/appointmentService'

vi.mock('@/services/appointmentService')
vi.mock('@/components/map/AppointmentMap.vue', () => ({
  default: {
    name: 'AppointmentMap',
    props: ['markers'],
    template: '<div class="appointment-map-stub" />',
  },
}))

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('OverviewView', () => {
  it('renders the daily appointment list', async () => {
    const wrapper = mount(OverviewView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()

    expect(wrapper.find('.daily-appointment-list').exists()).toBe(true)
  })

  it('renders the appointment map card next to the daily appointment list', async () => {
    const wrapper = mount(OverviewView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()

    expect(wrapper.find('.appointment-map-card').exists()).toBe(true)
  })

  it('renders the English empty-state text when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(OverviewView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()

    expect(wrapper.find('.daily-appointment-list__empty').text()).toBe(
      'No further appointments are scheduled for today.',
    )
  })
})
