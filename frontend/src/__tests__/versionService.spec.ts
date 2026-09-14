import { afterEach, describe, it, expect, vi } from 'vitest'
import axios from 'axios'
import { fetchAppVersion } from '@/services/versionService'

vi.mock('axios')

afterEach(() => {
  vi.mocked(axios.get).mockReset()
})

describe('versionService', () => {
  it('fetches the application version from the backend', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: { version: '0.1.0-SNAPSHOT' } })

    const version = await fetchAppVersion()

    expect(version).toBe('0.1.0-SNAPSHOT')
    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/api/version'))
  })
})
