import { afterEach, describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import { useNavigation } from '@/composables/useNavigation'
import NavigationMenu from '@/components/layout/NavigationMenu.vue'

afterEach(() => {
  useNavigation().closePopup()
})

describe('NavigationMenu', () => {
  it('renders every navigation entry label', () => {
    const wrapper = mount(NavigationMenu, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toContain('Übersicht')
    expect(wrapper.text()).toContain('Kalender')
    expect(wrapper.text()).toContain('Statistik')
    expect(wrapper.text()).toContain('Immobilien')
  })

  it('opens the shared coming soon popup when an entry is clicked', async () => {
    const wrapper = mount(NavigationMenu, {
      global: { plugins: [i18n] },
    })
    const { isPopupVisible } = useNavigation()

    expect(isPopupVisible.value).toBe(false)
    await wrapper.find('.navigation-menu__link').trigger('click')

    expect(isPopupVisible.value).toBe(true)
  })
})
