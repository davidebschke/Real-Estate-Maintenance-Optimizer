import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import OptimizationProposalList from '@/components/optimization/OptimizationProposalList.vue'
import OptimizationProposalCard from '@/components/optimization/OptimizationProposalCard.vue'
import { useOptimizationStore } from '@/stores/optimization'
import * as optimizationService from '@/services/optimizationService'
import { createProposal } from '@/__tests__/optimizationFixtures'

vi.mock('@/services/optimizationService')

const proposals = [createProposal({ id: 'proposal-1' }), createProposal({ id: 'proposal-2', appointmentTitle: 'Gartenpflege' })]

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  vi.mocked(optimizationService.fetchPendingProposals).mockReset().mockResolvedValue(proposals)
  vi.mocked(optimizationService.acceptProposal).mockReset().mockResolvedValue(proposals[0]!)
  vi.mocked(optimizationService.rejectProposal).mockReset().mockResolvedValue(proposals[1]!)
})

async function mountList() {
  await useOptimizationStore().fetchPendingProposals()
  const wrapper = mount(OptimizationProposalList, { global: { plugins: [PrimeVue, i18n] } })
  await flushPromises()
  return wrapper
}

describe('OptimizationProposalList', () => {
  it('renders one card per pending proposal and their count', async () => {
    const wrapper = await mountList()

    expect(wrapper.findAllComponents(OptimizationProposalCard)).toHaveLength(2)
    expect(wrapper.find('.optimization-proposal-list__count').text()).toBe('2')
    expect(wrapper.find('.optimization-proposal-list__heading').text()).toContain('Vorschläge zur Bestätigung')
  })

  it('shows an empty state without proposals', async () => {
    vi.mocked(optimizationService.fetchPendingProposals).mockResolvedValue([])
    const wrapper = await mountList()

    expect(wrapper.find('.optimization-proposal-list__empty').text()).toBe(
      'Keine offenen Vorschläge. Starten Sie eine Optimierung, um neue zu erhalten.',
    )
  })

  it('accepts and rejects a proposal through the store, removing its card', async () => {
    const wrapper = await mountList()

    await wrapper.findAll('.optimization-proposal-card__accept')[0]!.trigger('click')
    await flushPromises()
    await wrapper.find('.optimization-proposal-card__reject').trigger('click')
    await flushPromises()

    expect(optimizationService.acceptProposal).toHaveBeenCalledWith('proposal-1')
    expect(optimizationService.rejectProposal).toHaveBeenCalledWith('proposal-2')
    expect(wrapper.findAllComponents(OptimizationProposalCard)).toHaveLength(0)
  })

  it('shows the load error and the backend message of a failed decision', async () => {
    const wrapper = await mountList()
    const store = useOptimizationStore()
    store.hasLoadError = true
    store.hasDecisionError = true
    store.decisionErrorMessage = 'Dieser Vorschlag ist veraltet.'
    await flushPromises()

    const errors = wrapper.findAll('.optimization-proposal-list__error').map((node) => node.text())
    expect(errors).toEqual(['Die Vorschläge konnten nicht geladen werden.', 'Dieser Vorschlag ist veraltet.'])
  })

  it('falls back to a generic decision error without a backend message', async () => {
    const wrapper = await mountList()
    useOptimizationStore().hasDecisionError = true
    await flushPromises()

    expect(wrapper.find('.optimization-proposal-list__error').text()).toBe('Die Entscheidung konnte nicht gespeichert werden.')
  })
})
