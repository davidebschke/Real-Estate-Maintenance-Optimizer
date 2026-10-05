import { describe, expect, it } from 'vitest'
import { buildGoogleMapsDirectionsUrl } from '@/utils/googleMapsLink'

describe('buildGoogleMapsDirectionsUrl', () => {
  it('links to the directions view with the address as destination and no fixed origin', () => {
    const url = new URL(buildGoogleMapsDirectionsUrl('Aachener Str. 512, 50933 Köln', 'car'))

    expect(url.origin + url.pathname).toBe('https://www.google.com/maps/dir/')
    expect(url.searchParams.get('api')).toBe('1')
    expect(url.searchParams.get('destination')).toBe('Aachener Str. 512, 50933 Köln')
    expect(url.searchParams.has('origin')).toBe(false)
  })

  it('maps the route mode to the matching Google travel mode', () => {
    const carUrl = new URL(buildGoogleMapsDirectionsUrl('Musterweg 1, 50667 Köln', 'car'))
    const walkingUrl = new URL(buildGoogleMapsDirectionsUrl('Musterweg 1, 50667 Köln', 'walking'))

    expect(carUrl.searchParams.get('travelmode')).toBe('driving')
    expect(walkingUrl.searchParams.get('travelmode')).toBe('walking')
  })

  it('encodes characters that would break the query string', () => {
    const url = buildGoogleMapsDirectionsUrl('Str. 1 & 2, 50667 Köln', 'car')

    expect(url).not.toContain(' ')
    expect(new URL(url).searchParams.get('destination')).toBe('Str. 1 & 2, 50667 Köln')
  })
})
