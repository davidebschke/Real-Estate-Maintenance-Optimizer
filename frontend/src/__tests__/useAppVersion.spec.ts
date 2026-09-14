import { afterEach, describe, it, expect, vi } from 'vitest'
import { fetchAppVersion } from '@/services/versionService'
import { useAppVersion } from '@/composables/useAppVersion'

vi.mock('@/services/versionService', () => ({
  fetchAppVersion: vi.fn(),
}))

afterEach(() => {
  vi.mocked(fetchAppVersion).mockReset()
})

describe('useAppVersion', () => {
  it('loads and exposes the backend application version', async () => {
    vi.mocked(fetchAppVersion).mockResolvedValue('0.1.0-SNAPSHOT')
    const { version, isLoading, loadVersion } = useAppVersion()

    const pending = loadVersion()
    expect(isLoading.value).toBe(true)
    await pending

    expect(version.value).toBe('0.1.0-SNAPSHOT')
    expect(isLoading.value).toBe(false)
  })

  it('exposes an error message when loading fails', async () => {
    vi.mocked(fetchAppVersion).mockRejectedValue(new Error('network error'))
    const { version, error, loadVersion } = useAppVersion()

    await loadVersion()

    expect(version.value).toBeNull()
    expect(error.value).not.toBeNull()
  })
})
