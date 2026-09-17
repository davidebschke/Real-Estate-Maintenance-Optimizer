import axios from 'axios'

/** A resolved geographic position (WGS 84). */
export interface GeocodedPosition {
  lat: number
  lng: number
}

const nominatimBaseUrl = 'https://nominatim.openstreetmap.org/search'
const minDelayBetweenRequestsMs = 1000

const cache = new Map<string, GeocodedPosition | null>()
let nextRequestAt = 0

/** Delays execution until the given timestamp is reached, honoring Nominatim's rate limit. */
async function waitUntil(timestampMs: number) {
  const delayMs = timestampMs - Date.now()
  if (delayMs > 0) {
    await new Promise((resolve) => setTimeout(resolve, delayMs))
  }
}

/** Resolves a postal address to geographic coordinates, caching results per address for the session. */
export async function geocodeAddress(address: string): Promise<GeocodedPosition | null> {
  if (cache.has(address)) {
    return cache.get(address) ?? null
  }

  await waitUntil(nextRequestAt)
  nextRequestAt = Date.now() + minDelayBetweenRequestsMs

  let position: GeocodedPosition | null = null
  try {
    const { data } = await axios.get<Array<{ lat: string; lon: string }>>(nominatimBaseUrl, {
      params: { format: 'json', q: address, limit: 1 },
    })
    const [result] = data
    position = result
      ? { lat: Number.parseFloat(result.lat), lng: Number.parseFloat(result.lon) }
      : null
  } catch {
    position = null
  }

  cache.set(address, position)
  return position
}
