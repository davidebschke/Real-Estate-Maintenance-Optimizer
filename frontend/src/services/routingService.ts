import axios from 'axios'
import type { GeocodedPosition } from '@/services/geocodingService'
import type { RoadRoute, RouteLeg, RouteMode } from '@/types/route'

/** Shape of a route as returned by the backend, with GeoJSON-ordered `[lng, lat]` geometry. */
interface RouteResponseDto {
  geometry: [number, number][]
  distanceMeters: number
  durationSeconds: number
  legs: RouteLeg[]
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

const MAX_REMEMBERED_ROUTES = 50
const routeRequests = new Map<string, Promise<RoadRoute>>()

/** Fetches the route for the given mode through the given stops in order via the backend, sharing one request per identical mode and stop sequence and rejecting a failure without remembering it. */
export function fetchRoute(stops: GeocodedPosition[], mode: RouteMode): Promise<RoadRoute> {
  const key = `${mode}|${stops.map((stop) => `${stop.lat},${stop.lng}`).join(';')}`
  const remembered = routeRequests.get(key)
  if (remembered) {
    return remembered
  }

  if (routeRequests.size >= MAX_REMEMBERED_ROUTES) {
    routeRequests.clear()
  }
  const request = requestRoute(stops, mode).catch((error: unknown) => {
    routeRequests.delete(key)
    throw error
  })
  routeRequests.set(key, request)
  return request
}

/** Asks the backend for the route through the given stops and converts its geometry to Leaflet's `[lat, lng]` order. */
async function requestRoute(stops: GeocodedPosition[], mode: RouteMode): Promise<RoadRoute> {
  const { data } = await axios.post<RouteResponseDto>(`${apiBaseUrl}/api/routes`, {
    coordinates: stops.map((stop) => ({ latitude: stop.lat, longitude: stop.lng })),
    mode: mode === 'walking' ? 'WALKING' : 'CAR',
  })
  return {
    geometry: data.geometry.map(([lng, lat]): [number, number] => [lat, lng]),
    distanceMeters: data.distanceMeters,
    durationSeconds: data.durationSeconds,
    legs: data.legs,
  }
}
