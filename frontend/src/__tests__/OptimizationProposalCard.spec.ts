import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import OptimizationProposalCard from '@/components/optimization/OptimizationProposalCard.vue'
import { createProposal } from '@/__tests__/optimizationFixtures'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

function mountCard(overrides: Parameters<typeof createProposal>[0] = {}, deciding = false) {
  return mount(OptimizationProposalCard, {
    props: { proposal: createProposal(overrides), deciding },
    global: { plugins: [PrimeVue, i18n] },
  })
}

describe('OptimizationProposalCard', () => {
  it('shows the appointment, its current and proposed slot, the saving and the AI reason', () => {
    const wrapper = mountCard()

    expect(wrapper.find('.optimization-proposal-card__title').text()).toBe('Heizungswartung')
    expect(wrapper.find('.optimization-proposal-card__property').text()).toBe('Rheinhaus Deutz')
    expect(wrapper.find('.optimization-proposal-card__original').text()).toBe('03.11.2026, 10:00–11:00')
    expect(wrapper.find('.optimization-proposal-card__proposed').text()).toBe('04.11.2026, 09:15–10:15')
    expect(wrapper.find('.optimization-proposal-card__saving').text()).toBe('Ersparnis: 10 km · 15 Min')
    expect(wrapper.find('.optimization-proposal-card__reason').text()).toBe(
      'Begründung der KI: Direkt nach dem Termin im selben Objekt.',
    )
    expect(wrapper.find('.optimization-proposal-card__badge').exists()).toBe(false)
  })

  it('marks a recurring appointment and hides an empty reason', () => {
    const wrapper = mountCard({ recurring: true, reason: '' })

    expect(wrapper.find('.optimization-proposal-card__badge').text()).toBe('Wiederkehrend')
    expect(wrapper.find('.optimization-proposal-card__reason').exists()).toBe(false)
  })

  it('renders the English texts and number format when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mountCard({ savedDistanceMeters: 6_400, savedDurationSeconds: 4_000 })

    expect(wrapper.find('.optimization-proposal-card__saving').text()).toBe('Saving: 6.4 km · 1 hr 7 min')
    expect(wrapper.find('.optimization-proposal-card__accept').text()).toBe('Apply')
    expect(wrapper.find('.optimization-proposal-card__reject').text()).toBe('Decline')
  })

  it('emits the proposal id when it is accepted or rejected', async () => {
    const wrapper = mountCard()

    await wrapper.find('.optimization-proposal-card__accept').trigger('click')
    await wrapper.find('.optimization-proposal-card__reject').trigger('click')

    expect(wrapper.emitted('accept')).toEqual([['proposal-1']])
    expect(wrapper.emitted('reject')).toEqual([['proposal-1']])
  })

  it('disables both buttons while a decision is in flight', () => {
    const wrapper = mountCard({}, true)

    expect(wrapper.find('.optimization-proposal-card__accept').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.optimization-proposal-card__reject').attributes('disabled')).toBeDefined()
  })
})
