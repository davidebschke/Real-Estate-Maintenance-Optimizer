import { afterEach, describe, it, expect } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import StatisticsView from '@/views/StatisticsView.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

async function mountView() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push({ name: 'statistics' })
  const wrapper = mount(StatisticsView, { global: { plugins: [i18n, router] } })
  await flushPromises()
  return wrapper
}

describe('StatisticsView', () => {
  it('renders the localized heading and the statistics menu', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('.statistics-view__heading').text()).toBe('Statistik')
    expect(wrapper.find('.statistics-menu').exists()).toBe(true)
  })

  it('renders the English heading when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    const wrapper = await mountView()

    expect(wrapper.find('.statistics-view__heading').text()).toBe('Statistics')
  })
})
