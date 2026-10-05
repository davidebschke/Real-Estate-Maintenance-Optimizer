import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import { useAppointmentRoute } from '@/composables/useAppointmentRoute'
import * as routingService from '@/services/routingService'
import type { AppointmentMapMarker } from '@/composables/useAppointmentMapMarkers'
import type { RoadRoute } from '@/types/route'

vi.mock('@/services/routingService')

/** Builds a map marker for the appointment with the given id at the given position. */
function createMarker(id: string, lat: number, lng: number): AppointmentMapMarker {
  return { appointment: { id } as never, position: Number(id), lat, lng }
}

/** Builds a road route with one leg per gap between the given number of stops. */
function createRoute(stopCount: number): RoadRoute {
  return {
    geometry: [
      [50.9, 6.9],
      [50.8, 6.8],
    ],
    distanceMeters: 1000 * (stopCount - 1),
    durationSeconds: 100 * (stopCount - 1),
    legs: Array.from({ length: stopCount - 1 }, (_, index) => ({
      distanceMeters: 1000 * (index + 1),
      durationSeconds: 100 * (index + 1),
    })),
  }
}

beforeEach(() => {
  vi.mocked(routingService.fetchRoute)
    .mockReset()
    .mockImplementation(async (stops) => createRoute(stops.length))
})

describe('useAppointmentRoute', () => {
  it('requests the route through the marker positions in order and exposes it', async () => {
    const markers = ref([createMarker('1', 50.9, 6.9), createMarker('2', 50.8, 6.8)])

    const { route, hasRouteError } = useAppointmentRoute(markers)
    await flushPromises()

    expect(routingService.fetchRoute).toHaveBeenCalledWith([
      { lat: 50.9, lng: 6.9 },
      { lat: 50.8, lng: 6.8 },
    ])
    expect(route.value).toEqual(createRoute(2))
    expect(hasRouteError.value).toBe(false)
  })

  it('does not request a route for fewer than two stops', async () => {
    const markers = ref([createMarker('1', 50.9, 6.9)])

    const { route, hasRouteError } = useAppointmentRoute(markers)
    await flushPromises()

    expect(routingService.fetchRoute).not.toHaveBeenCalled()
    expect(route.value).toBeNull()
    expect(hasRouteError.value).toBe(false)
  })

  it('assigns each appointment after the first the leg driven to reach it', async () => {
    const markers = ref([
      createMarker('1', 50.9, 6.9),
      createMarker('2', 50.8, 6.8),
      createMarker('3', 50.7, 6.7),
    ])

    const { legsByAppointmentId } = useAppointmentRoute(markers)
    await flushPromises()

    expect(legsByAppointmentId.value.has('1')).toBe(false)
    expect(legsByAppointmentId.value.get('2')).toEqual({
      distanceMeters: 1000,
      durationSeconds: 100,
    })
    expect(legsByAppointmentId.value.get('3')).toEqual({
      distanceMeters: 2000,
      durationSeconds: 200,
    })
  })

  it('treats consecutive appointments at the same position as one stop without travel', async () => {
    const markers = ref([
      createMarker('1', 50.9, 6.9),
      createMarker('2', 50.9, 6.9),
      createMarker('3', 50.7, 6.7),
    ])

    const { legsByAppointmentId } = useAppointmentRoute(markers)
    await flushPromises()

    expect(routingService.fetchRoute).toHaveBeenCalledWith([
      { lat: 50.9, lng: 6.9 },
      { lat: 50.7, lng: 6.7 },
    ])
    expect(legsByAppointmentId.value.get('2')).toEqual({ distanceMeters: 0, durationSeconds: 0 })
    expect(legsByAppointmentId.value.get('3')).toEqual({
      distanceMeters: 1000,
      durationSeconds: 100,
    })
  })

  it('does not request the route again when the markers change but their positions stay the same', async () => {
    const markers = ref([createMarker('1', 50.9, 6.9), createMarker('2', 50.8, 6.8)])
    useAppointmentRoute(markers)
    await flushPromises()

    markers.value = [createMarker('1', 50.9, 6.9), createMarker('2', 50.8, 6.8)]
    await flushPromises()

    expect(routingService.fetchRoute).toHaveBeenCalledTimes(1)
  })

  it('requests the route again when the sequence of positions changes', async () => {
    const markers = ref([createMarker('1', 50.9, 6.9), createMarker('2', 50.8, 6.8)])
    useAppointmentRoute(markers)
    await flushPromises()

    markers.value = [createMarker('2', 50.8, 6.8), createMarker('1', 50.9, 6.9)]
    await flushPromises()

    expect(routingService.fetchRoute).toHaveBeenCalledTimes(2)
  })

  it('reports an error and exposes no route when the calculation fails', async () => {
    vi.mocked(routingService.fetchRoute).mockRejectedValue(new Error('503'))
    const markers = ref([createMarker('1', 50.9, 6.9), createMarker('2', 50.8, 6.8)])

    const { route, hasRouteError, legsByAppointmentId, isLoading } = useAppointmentRoute(markers)
    await flushPromises()

    expect(route.value).toBeNull()
    expect(hasRouteError.value).toBe(true)
    expect(legsByAppointmentId.value.size).toBe(0)
    expect(isLoading.value).toBe(false)
  })

  it('clears the error once the positions change to a sequence that no longer needs a route', async () => {
    vi.mocked(routingService.fetchRoute).mockRejectedValue(new Error('503'))
    const markers = ref([createMarker('1', 50.9, 6.9), createMarker('2', 50.8, 6.8)])
    const { hasRouteError } = useAppointmentRoute(markers)
    await flushPromises()

    markers.value = [createMarker('1', 50.9, 6.9)]
    await flushPromises()

    expect(hasRouteError.value).toBe(false)
  })

  it('ignores a slower answer for positions that have been replaced in the meantime', async () => {
    let resolveFirst: (route: RoadRoute) => void = () => {}
    vi.mocked(routingService.fetchRoute)
      .mockImplementationOnce(() => new Promise<RoadRoute>((resolve) => (resolveFirst = resolve)))
      .mockImplementationOnce(async (stops) => createRoute(stops.length))
    const markers = ref([createMarker('1', 50.9, 6.9), createMarker('2', 50.8, 6.8)])
    const { route } = useAppointmentRoute(markers)

    markers.value = [
      createMarker('1', 50.9, 6.9),
      createMarker('2', 50.8, 6.8),
      createMarker('3', 50.7, 6.7),
    ]
    await flushPromises()
    resolveFirst(createRoute(2))
    await flushPromises()

    expect(route.value).toEqual(createRoute(3))
  })
})
