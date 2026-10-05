/** The way of travelling a route is calculated for. */
export type RouteMode = 'car' | 'walking'

/** Distance in meters and driving time in seconds between two consecutive stops of a road route. */
export interface RouteLeg {
  distanceMeters: number
  durationSeconds: number
}

/** A calculated road route; `geometry` holds Leaflet-ordered `[lat, lng]` pairs. */
export interface RoadRoute {
  geometry: [number, number][]
  distanceMeters: number
  durationSeconds: number
  legs: RouteLeg[]
}
