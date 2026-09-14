import { describe, it, expect } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import NavigationMenu from '@/components/layout/NavigationMenu.vue'

function createTestRouter() {
  return createRouter({ history: createMemoryHistory(), routes })
}

describe('NavigationMenu', () => {
  it('renders every navigation entry label', async () => {
    const router = createTestRouter()
    const wrapper = mount(NavigationMenu, {
      global: { plugins: [i18n, router] },
    })
    await router.isReady()

    expect(wrapper.text()).toContain('Übersicht')
    expect(wrapper.text()).toContain('Kalender')
    expect(wrapper.text()).toContain('Statistik')
    expect(wrapper.text()).toContain('Immobilien')
  })

  it('navigates to the matching route when an entry is clicked', async () => {
    const router = createTestRouter()
    const wrapper = mount(NavigationMenu, {
      global: { plugins: [i18n, router] },
    })
    await router.isReady()

    const calendarLink = wrapper.findAll('.navigation-menu__link')[1]
    await calendarLink!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('calendar')
  })

  it('marks the link matching the current route as active', async () => {
    const router = createTestRouter()
    await router.push({ name: 'statistics' })
    const wrapper = mount(NavigationMenu, {
      global: { plugins: [i18n, router] },
    })
    await router.isReady()

    const statisticsLink = wrapper.findAll('.navigation-menu__link')[2]

    expect(statisticsLink!.classes()).toContain('router-link-exact-active')
  })

  it('toggles the collapsed mobile navigation list open and closed', async () => {
    const router = createTestRouter()
    const wrapper = mount(NavigationMenu, {
      global: { plugins: [i18n, router] },
    })
    await router.isReady()
    const toggle = wrapper.find('.navigation-menu__toggle')

    expect(wrapper.find('.navigation-menu__list').classes()).not.toContain('navigation-menu__list--open')

    await toggle.trigger('click')

    expect(wrapper.find('.navigation-menu__list').classes()).toContain('navigation-menu__list--open')

    await toggle.trigger('click')

    expect(wrapper.find('.navigation-menu__list').classes()).not.toContain('navigation-menu__list--open')
  })

  it('closes the mobile navigation list when an entry is selected', async () => {
    const router = createTestRouter()
    const wrapper = mount(NavigationMenu, {
      global: { plugins: [i18n, router] },
    })
    await router.isReady()

    await wrapper.find('.navigation-menu__toggle').trigger('click')
    expect(wrapper.find('.navigation-menu__list').classes()).toContain('navigation-menu__list--open')

    await wrapper.find('.navigation-menu__link').trigger('click')

    expect(wrapper.find('.navigation-menu__list').classes()).not.toContain('navigation-menu__list--open')
  })
})
