import { afterEach, describe, it, expect, vi } from 'vitest'
import axios from 'axios'
import { fetchProperties } from '@/services/propertyService'

vi.mock('axios')

afterEach(() => {
  vi.mocked(axios.get).mockReset()
})

describe('propertyService', () => {
  it('fetches every property', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: [{ id: '1', name: 'Wohnanlage Sonnenhof', address: 'Aachener Str. 512', icon: 'pi-building' }],
    })

    const properties = await fetchProperties()

    expect(properties).toEqual([
      { id: '1', name: 'Wohnanlage Sonnenhof', address: 'Aachener Str. 512', icon: 'pi-building' },
    ])
    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/api/properties'))
  })
})
