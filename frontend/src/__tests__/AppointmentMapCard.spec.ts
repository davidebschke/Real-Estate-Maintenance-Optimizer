import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { computed, ref } from 'vue'
import { i18n } from '@/i18n'
import AppointmentMapCard from '@/components/map/AppointmentMapCard.vue'
import { useAppointmentMapMarkers } from '@/composables/useAppointmentMapMarkers'
import { useAppointmentRoute } from '@/composables/useAppointmentRoute'
import type { RoadRoute } from '@/types/route'
import * as propertyService from '@/services/propertyService'

vi.mock('@/composables/useAppointmentMapMarkers')
vi.mock('@/composables/useAppointmentRoute')
vi.mock('@/services/propertyService')
vi.mock('@/components/map/AppointmentMap.vue', () => ({
  default: {
    name: 'AppointmentMap',
    props: ['markers', 'route'],
    template: '<div class="appointment-map-stub" />',
  },
}))

/** Makes the route composable report the given route and error state. */
function mockRoute(route: RoadRoute | null, hasRouteError: boolean) {
  vi.mocked(useAppointmentRoute).mockReturnValue({
    route: ref(route),
    hasRouteError: ref(hasRouteError),
    isLoading: ref(false),
    legsByAppointmentId: computed(() => new Map()),
  })
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(propertyService.fetchProperties).mockResolvedValue([])
  mockRoute(null, false)
  vi.mocked(useAppointmentMapMarkers).mockReturnValue({
    markers: ref([]),
    isLoading: ref(false),
    hasGeocodingError: ref(false),
    allTodaysAppointmentsCompleted: computed(() => false),
  })
})

afterEach(() => {
  i18n.global.locale.value = 'de'
  vi.mocked(useAppointmentMapMarkers).mockReset()
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
      allTodaysAppointmentsCompleted: computed(() => false),
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
      allTodaysAppointmentsCompleted: computed(() => false),
    })

    const wrapper = mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(wrapper.find('.appointment-map-card__hint').text()).toBe(
      'Für einzelne Adressen konnte kein Standort ermittelt werden.',
    )
  })

  it('hands the road route geometry to the map', () => {
    mockRoute(
      {
        geometry: [
          [50.9, 6.9],
          [50.95, 6.93],
        ],
        distanceMeters: 5000,
        durationSeconds: 600,
        legs: [],
      },
      false,
    )
    vi.mocked(useAppointmentMapMarkers).mockReturnValue({
      markers: ref([{ appointment: { id: '1' } as never, position: 1, lat: 50.9, lng: 6.9 }]),
      isLoading: ref(false),
      hasGeocodingError: ref(false),
      allTodaysAppointmentsCompleted: computed(() => false),
    })

    const wrapper = mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(wrapper.findComponent({ name: 'AppointmentMap' }).props('route')).toEqual([
      [50.9, 6.9],
      [50.95, 6.93],
    ])
  })

  it('shows a hint that the straight line is displayed when the road route is unavailable', () => {
    mockRoute(null, true)
    vi.mocked(useAppointmentMapMarkers).mockReturnValue({
      markers: ref([{ appointment: { id: '1' } as never, position: 1, lat: 50.9, lng: 6.9 }]),
      isLoading: ref(false),
      hasGeocodingError: ref(false),
      allTodaysAppointmentsCompleted: computed(() => false),
    })

    const wrapper = mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(wrapper.find('.appointment-map-card__route-hint').text()).toBe(
      'Die Straßenroute ist gerade nicht verfügbar, die Karte zeigt die Luftlinie.',
    )
    expect(wrapper.find('.appointment-map-stub').exists()).toBe(true)
  })

  it('shows no route hint while the road route is available', () => {
    vi.mocked(useAppointmentMapMarkers).mockReturnValue({
      markers: ref([{ appointment: { id: '1' } as never, position: 1, lat: 50.9, lng: 6.9 }]),
      isLoading: ref(false),
      hasGeocodingError: ref(false),
      allTodaysAppointmentsCompleted: computed(() => false),
    })

    const wrapper = mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(wrapper.find('.appointment-map-card__route-hint').exists()).toBe(false)
  })

  it('shows a checkmark instead of the map once every appointment today is completed', () => {
    vi.mocked(useAppointmentMapMarkers).mockReturnValue({
      markers: ref([{ appointment: { id: '1' } as never, position: 1, lat: 50.9, lng: 6.9 }]),
      isLoading: ref(false),
      hasGeocodingError: ref(false),
      allTodaysAppointmentsCompleted: computed(() => true),
    })

    const wrapper = mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(wrapper.find('.appointment-map-card__completed').exists()).toBe(true)
    expect(wrapper.find('.appointment-map-card__completed-text').text()).toBe(
      'Alle Termine heute erledigt',
    )
    expect(wrapper.find('.appointment-map-stub').exists()).toBe(false)
    expect(wrapper.find('.appointment-map-card__empty').exists()).toBe(false)
  })

  it('loads the properties store on mount', () => {
    mount(AppointmentMapCard, { global: { plugins: [i18n] } })

    expect(propertyService.fetchProperties).toHaveBeenCalled()
  })
})
