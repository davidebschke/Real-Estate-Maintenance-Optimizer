import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import L from 'leaflet'
import PropertyLocationPreviewMap from '@/components/properties/PropertyLocationPreviewMap.vue'

interface MapMock {
  setView: ReturnType<typeof vi.fn<(center: unknown, zoom: number) => MapMock>>
  remove: ReturnType<typeof vi.fn<() => void>>
}
interface MarkerMock {
  addTo: ReturnType<typeof vi.fn<(map: unknown) => MarkerMock>>
  remove: ReturnType<typeof vi.fn<() => void>>
}

const mapInstance: MapMock = {
  setView: vi.fn<(center: unknown, zoom: number) => MapMock>(),
  remove: vi.fn<() => void>(),
}
const tileLayerInstance = { addTo: vi.fn<(map: unknown) => void>() }
const markerInstance: MarkerMock = {
  addTo: vi.fn<(map: unknown) => MarkerMock>(),
  remove: vi.fn<() => void>(),
}

vi.mock('leaflet', () => ({
  default: {
    map: vi.fn<() => typeof mapInstance>(() => mapInstance),
    tileLayer: vi.fn<() => typeof tileLayerInstance>(() => tileLayerInstance),
    marker: vi.fn<() => typeof markerInstance>(() => markerInstance),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
  mapInstance.setView.mockReturnValue(mapInstance)
  markerInstance.addTo.mockReturnValue(markerInstance)
})

afterEach(() => {
  vi.clearAllMocks()
})

describe('PropertyLocationPreviewMap', () => {
  it('centers on the default view when no position is given', () => {
    mount(PropertyLocationPreviewMap, { props: { position: null } })

    expect(L.marker).not.toHaveBeenCalled()
    expect(mapInstance.setView).toHaveBeenCalledWith([51.1657, 10.4515], 6)
  })

  it('places a marker and centers on it once a position is given', () => {
    mount(PropertyLocationPreviewMap, { props: { position: { lat: 50.94, lng: 6.88 } } })

    expect(L.marker).toHaveBeenCalledWith([50.94, 6.88])
    expect(mapInstance.setView).toHaveBeenCalledWith([50.94, 6.88], 15)
  })

  it('replaces the marker when the position changes', async () => {
    const wrapper = mount(PropertyLocationPreviewMap, {
      props: { position: { lat: 50.94, lng: 6.88 } },
    })

    await wrapper.setProps({ position: { lat: 51, lng: 7 } })

    expect(markerInstance.remove).toHaveBeenCalledTimes(1)
    expect(L.marker).toHaveBeenCalledWith([51, 7])
  })

  it('removes the map instance when unmounted', () => {
    const wrapper = mount(PropertyLocationPreviewMap, { props: { position: null } })

    wrapper.unmount()

    expect(mapInstance.remove).toHaveBeenCalled()
  })
})
