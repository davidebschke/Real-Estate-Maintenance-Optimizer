import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import OptimizationBanner from '@/components/optimization/OptimizationBanner.vue'
import * as optimizationService from '@/services/optimizationService'
import { createProposal } from '@/__tests__/optimizationFixtures'

vi.mock('@/services/optimizationService')

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(optimizationService.fetchPendingProposals).mockReset().mockResolvedValue([])
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

async function mountBanner() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push({ name: 'overview' })
  const wrapper = mount(OptimizationBanner, { global: { plugins: [i18n, router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('OptimizationBanner', () => {
  it('loads the pending proposals and invites to the optimization while there are none', async () => {
    const { wrapper } = await mountBanner()

    expect(optimizationService.fetchPendingProposals).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.optimization-banner__text').text()).toBe(
      'Die KI kann Ihre Termine ab vier Wochen im Voraus nach Fahrzeit und Kilometern optimieren.',
    )
    expect(wrapper.find('.optimization-banner__action').text()).toBe('Zur Optimierung')
    expect(wrapper.classes()).not.toContain('optimization-banner--pending')
  })

  it('counts the pending proposals and offers to review them', async () => {
    vi.mocked(optimizationService.fetchPendingProposals).mockResolvedValue([
      createProposal({ id: 'proposal-1' }),
      createProposal({ id: 'proposal-2' }),
    ])
    const { wrapper } = await mountBanner()

    expect(wrapper.find('.optimization-banner__text').text()).toBe('Die KI hat 2 Vorschläge, die Fahrzeit sparen.')
    expect(wrapper.find('.optimization-banner__action').text()).toBe('Vorschläge ansehen')
    expect(wrapper.classes()).toContain('optimization-banner--pending')
  })

  it('uses the singular for one proposal and the English texts when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    vi.mocked(optimizationService.fetchPendingProposals).mockResolvedValue([createProposal()])
    const { wrapper } = await mountBanner()

    expect(wrapper.find('.optimization-banner__text').text()).toBe('The AI has one proposal that saves driving time.')
    expect(wrapper.find('.optimization-banner__action').text()).toBe('Review proposals')
  })

  it('opens the optimization page', async () => {
    const { wrapper, router } = await mountBanner()

    await wrapper.find('.optimization-banner__action').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/optimization')
  })
})
