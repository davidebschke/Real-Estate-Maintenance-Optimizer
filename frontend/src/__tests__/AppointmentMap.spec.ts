import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import L from 'leaflet'
import AppointmentMap from '@/components/map/AppointmentMap.vue'
import type { AppointmentMapMarker } from '@/composables/useAppointmentMapMarkers'
import type { Appointment } from '@/types/appointment'

interface MapMock {
  setView: ReturnType<typeof vi.fn<(center: unknown, zoom: number) => MapMock>>
  remove: ReturnType<typeof vi.fn<() => void>>
  fitBounds: ReturnType<typeof vi.fn<(bounds: unknown, options?: unknown) => void>>
  invalidateSize: ReturnType<typeof vi.fn<() => void>>
}
interface LayerGroupMock {
  addTo: ReturnType<typeof vi.fn<(map: unknown) => LayerGroupMock>>
  clearLayers: ReturnType<typeof vi.fn<() => void>>
}
interface MarkerMock {
  bindPopup: ReturnType<typeof vi.fn<(content: string) => MarkerMock>>
  addTo: ReturnType<typeof vi.fn<(layer: unknown) => void>>
}

const mapInstance: MapMock = {
  setView: vi.fn<(center: unknown, zoom: number) => MapMock>(),
  remove: vi.fn<() => void>(),
  fitBounds: vi.fn<(bounds: unknown, options?: unknown) => void>(),
  invalidateSize: vi.fn<() => void>(),
}
const tileLayerInstance = { addTo: vi.fn<(map: unknown) => void>() }
const layerGroupInstance: LayerGroupMock = {
  addTo: vi.fn<(map: unknown) => LayerGroupMock>(),
  clearLayers: vi.fn<() => void>(),
}
const markerInstance: MarkerMock = {
  bindPopup: vi.fn<(content: string) => MarkerMock>(),
  addTo: vi.fn<(layer: unknown) => void>(),
}
const polylineInstance = { addTo: vi.fn<(map: unknown) => void>(), remove: vi.fn<() => void>() }

vi.mock('leaflet', () => ({
  default: {
    map: vi.fn<() => typeof mapInstance>(() => mapInstance),
    tileLayer: vi.fn<() => typeof tileLayerInstance>(() => tileLayerInstance),
    layerGroup: vi.fn<() => typeof layerGroupInstance>(() => layerGroupInstance),
    marker: vi.fn<() => typeof markerInstance>(() => markerInstance),
    polyline: vi.fn<() => typeof polylineInstance>(() => polylineInstance),
    latLngBounds: vi.fn<(latLngs: unknown) => unknown>((latLngs) => latLngs),
  },
}))

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: 'property-1',
    propertyName: 'Wohnanlage Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: '',
    category: 'maintenance',
    start: new Date(2026, 7, 10, 8, 30),
    end: new Date(2026, 7, 10, 10, 0),
    locked: false,
    recurring: false,
    recurrenceIntervalMonths: null,
    materials: [],
    history: [],
    travelDistanceKm: 6.4,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

function createMarker(overrides: Partial<AppointmentMapMarker> = {}): AppointmentMapMarker {
  return { appointment: createAppointment(), position: 1, lat: 50.9, lng: 6.9, ...overrides }
}

let resizeObserverCallback: (() => void) | null = null
const resizeObserverDisconnect = vi.fn<() => void>()

class ResizeObserverMock {
  constructor(callback: () => void) {
    resizeObserverCallback = callback
  }
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {
    resizeObserverDisconnect()
  }
}

beforeEach(() => {
  vi.clearAllMocks()
  mapInstance.setView.mockReturnValue(mapInstance)
  layerGroupInstance.addTo.mockReturnValue(layerGroupInstance)
  markerInstance.bindPopup.mockReturnValue(markerInstance)
  resizeObserverCallback = null
  vi.stubGlobal('ResizeObserver', ResizeObserverMock)
})

afterEach(() => {
  vi.clearAllMocks()
  vi.unstubAllGlobals()
})

describe('AppointmentMap', () => {
  it('draws a marker and a connecting route line for each appointment, then fits the view to them', () => {
    const markers = [
      createMarker({ position: 1, lat: 50.9, lng: 6.9 }),
      createMarker({ position: 2, lat: 50.8, lng: 6.8 }),
    ]

    mount(AppointmentMap, { props: { markers } })

    expect(L.marker).toHaveBeenCalledTimes(2)
    expect(L.polyline).toHaveBeenCalledWith(
      [
        [50.9, 6.9],
        [50.8, 6.8],
      ],
      expect.objectContaining({ color: expect.any(String) }),
    )
    expect(mapInstance.fitBounds).toHaveBeenCalledWith(
      [
        [50.9, 6.9],
        [50.8, 6.8],
      ],
      expect.objectContaining({ padding: expect.any(Array) }),
    )
  })

  it('does not draw a route line for a single appointment', () => {
    mount(AppointmentMap, { props: { markers: [createMarker()] } })

    expect(L.marker).toHaveBeenCalledTimes(1)
    expect(L.polyline).not.toHaveBeenCalled()
  })

  it('redraws the markers when the appointment list changes', async () => {
    const wrapper = mount(AppointmentMap, { props: { markers: [createMarker()] } })

    await wrapper.setProps({
      markers: [createMarker(), createMarker({ position: 2, lat: 51, lng: 7 })],
    })

    expect(layerGroupInstance.clearLayers).toHaveBeenCalled()
    expect(L.marker).toHaveBeenCalledTimes(3)
  })

  it('removes the map instance when unmounted', () => {
    const wrapper = mount(AppointmentMap, { props: { markers: [createMarker()] } })

    wrapper.unmount()

    expect(mapInstance.remove).toHaveBeenCalled()
  })

  it('invalidates the map size whenever its container is resized, and stops observing on unmount', () => {
    const wrapper = mount(AppointmentMap, { props: { markers: [createMarker()] } })

    resizeObserverCallback?.()

    expect(mapInstance.invalidateSize).toHaveBeenCalled()

    wrapper.unmount()

    expect(resizeObserverDisconnect).toHaveBeenCalled()
  })
})
