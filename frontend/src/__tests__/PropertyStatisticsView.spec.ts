import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import PropertyStatisticsView from '@/views/PropertyStatisticsView.vue'
import PropertyStatisticsList from '@/components/statistics/PropertyStatisticsList.vue'
import * as propertyService from '@/services/propertyService'
import * as appointmentService from '@/services/appointmentService'

vi.mock('@/services/propertyService')
vi.mock('@/services/appointmentService')

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(propertyService.fetchProperties).mockReset().mockResolvedValue([])
  vi.mocked(appointmentService.fetchAppointments).mockReset().mockResolvedValue([])
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

async function mountView() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push({ name: 'statistics-properties' })
  const wrapper = mount(PropertyStatisticsView, { global: { plugins: [i18n, router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('PropertyStatisticsView', () => {
  it('renders the localized heading and the property statistics list', async () => {
    const { wrapper } = await mountView()

    expect(wrapper.find('.property-statistics-view__heading').text()).toBe('Objektstatistik')
    expect(wrapper.findComponent(PropertyStatisticsList).exists()).toBe(true)
  })

  it('renders the English heading when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    const { wrapper } = await mountView()

    expect(wrapper.find('.property-statistics-view__heading').text()).toBe('Property statistics')
  })

  it('loads the properties and appointments it lists', async () => {
    await mountView()

    expect(propertyService.fetchProperties).toHaveBeenCalledTimes(1)
    expect(appointmentService.fetchAppointments).toHaveBeenCalledTimes(1)
  })

  it('shows a hint that the figures are incomplete when the appointments cannot be loaded', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockRejectedValue(new Error('network error'))
    const { wrapper } = await mountView()

    expect(wrapper.find('.property-statistics-view__error').text()).toBe(
      'Die Aufträge konnten nicht geladen werden, die Zahlen sind unvollständig.',
    )
  })

  it('shows no hint when the appointments load', async () => {
    const { wrapper } = await mountView()

    expect(wrapper.find('.property-statistics-view__error').exists()).toBe(false)
  })

  it('links back to the statistics page', async () => {
    const { wrapper, router } = await mountView()

    await wrapper.find('.property-statistics-view__back').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/statistics')
  })
})
