import { describe, it, expect, vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import AppFooter from '@/components/layout/AppFooter.vue'
import FooterVersion from '@/components/layout/FooterVersion.vue'

vi.mock('@/services/versionService', () => ({
  fetchAppVersion: vi.fn().mockResolvedValue('0.1.0-SNAPSHOT'),
}))

describe('AppFooter', () => {
  it('renders the copyright notice and composes the version component', () => {
    const wrapper = shallowMount(AppFooter, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toContain('Remo')
    expect(wrapper.findComponent(FooterVersion).exists()).toBe(true)
  })
})
