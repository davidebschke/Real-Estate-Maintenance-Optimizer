import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import OptimizationView from '@/views/OptimizationView.vue'
import OptimizationLaunchCard from '@/components/optimization/OptimizationLaunchCard.vue'
import OptimizationProposalList from '@/components/optimization/OptimizationProposalList.vue'
import * as optimizationService from '@/services/optimizationService'

vi.mock('@/services/optimizationService')
vi.mock('primevue/useconfirm')

beforeEach(() => {
  vi.mocked(useConfirm).mockReturnValue({ require: vi.fn() } as never)
  setActivePinia(createPinia())
  vi.mocked(optimizationService.fetchPendingProposals).mockReset().mockResolvedValue([])
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

async function mountView() {
  const wrapper = mount(OptimizationView, { global: { plugins: [PrimeVue, i18n] } })
  await flushPromises()
  return wrapper
}

describe('OptimizationView', () => {
  it('renders the heading, the explanation, the launch card and the proposals', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('.optimization-view__heading').text()).toBe('KI-Terminoptimierung')
    expect(wrapper.find('.optimization-view__intro').text()).toContain('Jede Verschiebung wird erst übernommen')
    expect(wrapper.findComponent(OptimizationLaunchCard).exists()).toBe(true)
    expect(wrapper.findComponent(OptimizationProposalList).exists()).toBe(true)
  })

  it('loads the pending proposals when opened', async () => {
    await mountView()

    expect(optimizationService.fetchPendingProposals).toHaveBeenCalledTimes(1)
  })

  it('renders the English heading when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    const wrapper = await mountView()

    expect(wrapper.find('.optimization-view__heading').text()).toBe('AI appointment optimization')
  })
})
