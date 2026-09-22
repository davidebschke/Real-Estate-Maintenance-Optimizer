import { afterEach, describe, it, expect, vi } from 'vitest'
import axios from 'axios'
import { createProperty, fetchProperties } from '@/services/propertyService'

vi.mock('axios')

afterEach(() => {
  vi.mocked(axios.get).mockReset()
  vi.mocked(axios.post).mockReset()
})

describe('propertyService', () => {
  it('fetches every property', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: [
        {
          id: '1',
          name: 'Wohnanlage Sonnenhof',
          address: 'Aachener Str. 512',
          icon: 'pi-building',
          latitude: 50.94,
          longitude: 6.88,
        },
      ],
    })

    const properties = await fetchProperties()

    expect(properties).toEqual([
      {
        id: '1',
        name: 'Wohnanlage Sonnenhof',
        address: 'Aachener Str. 512',
        icon: 'pi-building',
        latitude: 50.94,
        longitude: 6.88,
      },
    ])
    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/api/properties'))
  })

  it('creates a new property', async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        id: '2',
        name: 'Wohnanlage Nordpark',
        address: 'Nordparkstr. 3, 50733 Köln',
        icon: 'pi-building',
        latitude: 50.97,
        longitude: 6.95,
      },
    })

    const property = await createProperty({
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      latitude: 50.97,
      longitude: 6.95,
    })

    expect(property).toEqual({
      id: '2',
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      icon: 'pi-building',
      latitude: 50.97,
      longitude: 6.95,
    })
    expect(axios.post).toHaveBeenCalledWith(
      expect.stringContaining('/api/properties'),
      expect.objectContaining({ name: 'Wohnanlage Nordpark' }),
    )
  })
})
