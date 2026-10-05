import type { RouteMode } from '@/types/route'

const GOOGLE_MAPS_DIRECTIONS_URL = 'https://www.google.com/maps/dir/'

const GOOGLE_TRAVEL_MODES: Record<RouteMode, string> = {
  car: 'driving',
  walking: 'walking',
}

/** Builds a Google Maps directions link to the given address that starts at the user's current location, as Google does when no origin is passed. */
export function buildGoogleMapsDirectionsUrl(destinationAddress: string, mode: RouteMode): string {
  const parameters = new URLSearchParams({
    api: '1',
    destination: destinationAddress,
    travelmode: GOOGLE_TRAVEL_MODES[mode],
  })
  return `${GOOGLE_MAPS_DIRECTIONS_URL}?${parameters.toString()}`
}
