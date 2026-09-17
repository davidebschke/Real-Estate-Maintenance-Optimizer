import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest'
import axios from 'axios'

vi.mock('axios')

beforeEach(() => {
  vi.resetModules()
})

afterEach(() => {
  vi.mocked(axios.get).mockReset()
})

describe('geocodingService', () => {
  it('resolves an address to its coordinates', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [{ lat: '50.9333', lon: '6.9333' }] })
    const { geocodeAddress } = await import('@/services/geocodingService')

    const position = await geocodeAddress('Aachener Str. 512, 50933 Köln')

    expect(position).toEqual({ lat: 50.9333, lng: 6.9333 })
  })

  it('returns null when the address cannot be resolved', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [] })
    const { geocodeAddress } = await import('@/services/geocodingService')

    const position = await geocodeAddress('Unbekannte Adresse')

    expect(position).toBeNull()
  })

  it('returns null instead of throwing when the request fails', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('network error'))
    const { geocodeAddress } = await import('@/services/geocodingService')

    const position = await geocodeAddress('Fehlerhafte Adresse')

    expect(position).toBeNull()
  })

  it('caches the result per address instead of requesting it again', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [{ lat: '50.9333', lon: '6.9333' }] })
    const { geocodeAddress } = await import('@/services/geocodingService')

    await geocodeAddress('Aachener Str. 512, 50933 Köln')
    await geocodeAddress('Aachener Str. 512, 50933 Köln')

    expect(axios.get).toHaveBeenCalledTimes(1)
  })
})
