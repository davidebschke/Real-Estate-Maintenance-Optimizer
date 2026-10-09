import { afterEach, describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import StatisticsMenu from '@/components/statistics/StatisticsMenu.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

async function mountMenu() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push({ name: 'statistics' })
  const wrapper = mount(StatisticsMenu, { global: { plugins: [i18n, router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('StatisticsMenu', () => {
  it('lists the property statistics entry with its description', async () => {
    const { wrapper } = await mountMenu()

    const entries = wrapper.findAll('.statistics-menu__entry')
    expect(entries).toHaveLength(1)
    expect(entries[0]!.find('.statistics-menu__title').text()).toBe('Objektstatistik')
    expect(entries[0]!.find('.statistics-menu__description').text()).toBe(
      'Offene und erledigte Aufträge sowie der nächste Auftrag je Objekt',
    )
  })

  it('renders the English texts when the locale is switched', async () => {
    i18n.global.locale.value = 'en'
    const { wrapper } = await mountMenu()

    expect(wrapper.find('.statistics-menu__title').text()).toBe('Property statistics')
  })

  it('opens the property statistics when the entry is clicked', async () => {
    const { wrapper, router } = await mountMenu()

    await wrapper.find('.statistics-menu__entry').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/statistics/properties')
  })
})
