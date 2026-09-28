import { afterEach, describe, it, expect, vi } from 'vitest'
import axios from 'axios'
import { createProperty, deleteProperty, fetchProperties, updateProperty } from '@/services/propertyService'

vi.mock('axios')

afterEach(() => {
  vi.mocked(axios.get).mockReset()
  vi.mocked(axios.post).mockReset()
  vi.mocked(axios.put).mockReset()
  vi.mocked(axios.delete).mockReset()
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

  it('updates an existing property by id', async () => {
    vi.mocked(axios.put).mockResolvedValue({
      data: {
        id: '1',
        name: 'Wohnanlage Nordpark',
        address: 'Nordparkstr. 3, 50733 Köln',
        icon: 'pi-building',
        latitude: 50.97,
        longitude: 6.95,
      },
    })

    const property = await updateProperty('1', {
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      latitude: 50.97,
      longitude: 6.95,
    })

    expect(property).toEqual({
      id: '1',
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      icon: 'pi-building',
      latitude: 50.97,
      longitude: 6.95,
    })
    expect(axios.put).toHaveBeenCalledWith(
      expect.stringContaining('/api/properties/1'),
      expect.objectContaining({ name: 'Wohnanlage Nordpark' }),
    )
  })

  it('deletes a property by id', async () => {
    vi.mocked(axios.delete).mockResolvedValue({ data: undefined })

    await deleteProperty('1')

    expect(axios.delete).toHaveBeenCalledWith(expect.stringContaining('/api/properties/1'))
  })
})
