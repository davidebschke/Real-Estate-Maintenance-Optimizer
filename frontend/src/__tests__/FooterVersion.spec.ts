import { afterEach, describe, it, expect, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import { fetchAppVersion } from '@/services/versionService'
import FooterVersion from '@/components/layout/FooterVersion.vue'

vi.mock('@/services/versionService', () => ({
  fetchAppVersion: vi.fn(),
}))

afterEach(() => {
  vi.mocked(fetchAppVersion).mockReset()
})

describe('FooterVersion', () => {
  it('renders the version once it has loaded', async () => {
    vi.mocked(fetchAppVersion).mockResolvedValue('0.1.0-SNAPSHOT')

    const wrapper = mount(FooterVersion, { global: { plugins: [i18n] } })
    await flushPromises()

    expect(wrapper.text()).toContain('0.1.0-SNAPSHOT')
  })

  it('renders nothing while the version has not loaded yet', () => {
    vi.mocked(fetchAppVersion).mockReturnValue(new Promise(() => {}))

    const wrapper = mount(FooterVersion, { global: { plugins: [i18n] } })

    expect(wrapper.text()).toBe('')
  })
})
