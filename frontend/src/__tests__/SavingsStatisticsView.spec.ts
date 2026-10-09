import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { createMemoryHistory, createRouter } from 'vue-router'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import SavingsStatisticsView from '@/views/SavingsStatisticsView.vue'
import SavingsSummary from '@/components/statistics/SavingsSummary.vue'
import SavingsLineChart from '@/components/statistics/SavingsLineChart.vue'
import * as optimizationService from '@/services/optimizationService'
import { createSavingsStatistics } from '@/__tests__/optimizationFixtures'

vi.mock('@/services/optimizationService')

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(optimizationService.fetchSavingsStatistics).mockReset().mockResolvedValue(createSavingsStatistics())
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

async function mountView() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push({ name: 'statistics-savings' })
  const wrapper = mount(SavingsStatisticsView, {
    global: { plugins: [PrimeVue, i18n, router], stubs: { SavingsLineChart: true } },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('SavingsStatisticsView', () => {
  it('loads the weekly savings and shows their totals and line chart', async () => {
    const { wrapper } = await mountView()

    expect(optimizationService.fetchSavingsStatistics).toHaveBeenCalledWith('WEEK')
    expect(wrapper.find('.savings-statistics-view__heading').text()).toBe('Einsparungen durch KI-Optimierung')
    expect(wrapper.findComponent(SavingsSummary).exists()).toBe(true)
    expect(wrapper.findComponent(SavingsLineChart).exists()).toBe(true)
  })

  it('switches to monthly savings', async () => {
    const { wrapper } = await mountView()

    const monthButton = wrapper.findAll('.savings-statistics-view__granularity button').find((button) => button.text() === 'Monate')
    await monthButton!.trigger('click')
    await flushPromises()

    expect(optimizationService.fetchSavingsStatistics).toHaveBeenLastCalledWith('MONTH')
  })

  it('invites to the optimization instead of drawing an empty chart', async () => {
    vi.mocked(optimizationService.fetchSavingsStatistics).mockResolvedValue(
      createSavingsStatistics({ acceptedProposalCount: 0, totalSavedDistanceMeters: 0, totalSavedDurationSeconds: 0, periods: [] }),
    )
    const { wrapper, router } = await mountView()

    expect(wrapper.findComponent(SavingsLineChart).exists()).toBe(false)
    expect(wrapper.find('.savings-statistics-view__empty').text()).toContain('Noch keine Einsparungen.')

    await wrapper.find('.savings-statistics-view__cta').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/optimization')
  })

  it('shows a load error', async () => {
    vi.mocked(optimizationService.fetchSavingsStatistics).mockRejectedValue(new Error('offline'))
    const { wrapper } = await mountView()

    expect(wrapper.find('.savings-statistics-view__error').text()).toBe('Die Einsparungen konnten nicht geladen werden.')
  })

  it('links back to the statistics page', async () => {
    const { wrapper, router } = await mountView()

    await wrapper.find('.savings-statistics-view__back').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/statistics')
  })

  it('renders the English heading when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    const { wrapper } = await mountView()

    expect(wrapper.find('.savings-statistics-view__heading').text()).toBe('Savings from the AI optimization')
  })
})
