import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'

vi.mock('axios')

const stops = [
  { lat: 50.94, lng: 6.87 },
  { lat: 50.95, lng: 6.93 },
]

const routeResponse = {
  geometry: [
    [6.87, 50.94],
    [6.9, 50.945],
    [6.93, 50.95],
  ],
  distanceMeters: 5000.5,
  durationSeconds: 700,
  legs: [{ distanceMeters: 5000.5, durationSeconds: 700 }],
}

beforeEach(() => {
  vi.resetModules()
})

afterEach(() => {
  vi.mocked(axios.post).mockReset()
})

describe('routingService', () => {
  it('posts the stops in order and flips the geometry to Leaflet order', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: routeResponse })
    const { fetchRoute } = await import('@/services/routingService')

    const route = await fetchRoute(stops)

    expect(axios.post).toHaveBeenCalledWith(expect.stringContaining('/api/routes'), {
      coordinates: [
        { latitude: 50.94, longitude: 6.87 },
        { latitude: 50.95, longitude: 6.93 },
      ],
    })
    expect(route.geometry).toEqual([
      [50.94, 6.87],
      [50.945, 6.9],
      [50.95, 6.93],
    ])
    expect(route.distanceMeters).toBe(5000.5)
    expect(route.durationSeconds).toBe(700)
    expect(route.legs).toEqual([{ distanceMeters: 5000.5, durationSeconds: 700 }])
  })

  it('shares one request between concurrent and later calls for the same stop sequence', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: routeResponse })
    const { fetchRoute } = await import('@/services/routingService')

    const [first, second] = await Promise.all([fetchRoute(stops), fetchRoute([...stops])])
    await fetchRoute(stops)

    expect(second).toBe(first)
    expect(axios.post).toHaveBeenCalledTimes(1)
  })

  it('requests a different stop sequence separately', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: routeResponse })
    const { fetchRoute } = await import('@/services/routingService')

    await fetchRoute(stops)
    await fetchRoute([...stops].reverse())

    expect(axios.post).toHaveBeenCalledTimes(2)
  })

  it('rejects when the backend fails and retries on the next call instead of remembering the failure', async () => {
    vi.mocked(axios.post)
      .mockRejectedValueOnce(new Error('503'))
      .mockResolvedValue({ data: routeResponse })
    const { fetchRoute } = await import('@/services/routingService')

    await expect(fetchRoute(stops)).rejects.toThrow('503')
    const route = await fetchRoute(stops)

    expect(route.legs).toHaveLength(1)
    expect(axios.post).toHaveBeenCalledTimes(2)
  })
})
