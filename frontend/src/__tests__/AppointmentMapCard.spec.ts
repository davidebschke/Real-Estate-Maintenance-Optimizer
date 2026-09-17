import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import { i18n } from '@/i18n'
import AppointmentMapCard from '@/components/map/AppointmentMapCard.vue'
import { useAppointmentMapMarkers } from '@/composables/useAppointmentMapMarkers'

vi.mock('@/composables/useAppointmentMapMarkers')
vi.mock('@/components/map/AppointmentMap.vue', () => ({
  default: {
    name: 'AppointmentMap',
    props: ['markers'],
    template: '<div class="appointment-map-stub" />',
  },
}))

afterEach(() => {
  i18n.global.locale.value = 'de'
  vi.mocked(useAppointmentMapMarkers).mockReset()
})

beforeEach(() => {
  vi.mocked(useAppointmentMapMarkers).mockReturnValue({
    markers: ref([]),
    isLoading: ref(false),
    hasGeocodingError: ref(false),
  })
})

describe('AppointmentMapCard', () => {
  it('shows an empty-state message when there are no markers', () => {
    const wrapper = mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(wrapper.find('.appointment-map-card__empty').text()).toBe(
      'Heute stehen keine Termine an.',
    )
    expect(wrapper.find('.appointment-map-stub').exists()).toBe(false)
  })

  it('renders the map once markers are available', () => {
    vi.mocked(useAppointmentMapMarkers).mockReturnValue({
      markers: ref([
        {
          appointment: { id: '1', title: 'Heizungswartung' } as never,
          position: 1,
          lat: 50.9,
          lng: 6.9,
        },
      ]),
      isLoading: ref(false),
      hasGeocodingError: ref(false),
    })

    const wrapper = mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(wrapper.find('.appointment-map-stub').exists()).toBe(true)
    expect(wrapper.find('.appointment-map-card__empty').exists()).toBe(false)
  })

  it('shows a hint when some addresses could not be geocoded', () => {
    vi.mocked(useAppointmentMapMarkers).mockReturnValue({
      markers: ref([{ appointment: { id: '1' } as never, position: 1, lat: 50.9, lng: 6.9 }]),
      isLoading: ref(false),
      hasGeocodingError: ref(true),
    })

    const wrapper = mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(wrapper.find('.appointment-map-card__hint').text()).toBe(
      'Für einzelne Adressen konnte kein Standort ermittelt werden.',
    )
  })
})
