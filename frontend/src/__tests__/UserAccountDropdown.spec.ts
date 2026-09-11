import { afterEach, describe, it, expect } from 'vitest'
import { DOMWrapper, mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import UserAccountDropdown from '@/components/layout/UserAccountDropdown.vue'

let wrapper: ReturnType<typeof mount> | undefined

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
})

describe('UserAccountDropdown', () => {
  it('shows the default active account on the trigger', () => {
    wrapper = mount(UserAccountDropdown, {
      global: { plugins: [i18n, PrimeVue] },
    })

    expect(wrapper.find('.user-account-dropdown__trigger').text()).toContain('Marco Keller')
  })

  it('opens the popover with the account switcher when the trigger is clicked', async () => {
    wrapper = mount(UserAccountDropdown, {
      global: { plugins: [i18n, PrimeVue] },
      attachTo: document.body,
    })

    await wrapper.find('.user-account-dropdown__trigger').trigger('click')

    expect(document.body.textContent).toContain('m.keller@rmo-koeln.de')
    expect(document.body.textContent).toContain('Anna Braun')
    expect(document.body.textContent).toContain('Thomas Wagner')
  })

  it('switches the active account when another account is selected', async () => {
    wrapper = mount(UserAccountDropdown, {
      global: { plugins: [i18n, PrimeVue] },
      attachTo: document.body,
    })

    await wrapper.find('.user-account-dropdown__trigger').trigger('click')
    const options = [...document.body.querySelectorAll('.user-account-dropdown__account-option')]
    const annaOption = options.find((option) => option.textContent?.includes('Anna Braun'))

    await new DOMWrapper(annaOption as Element).trigger('click')

    expect(wrapper.find('.user-account-dropdown__trigger').text()).toContain('Anna Braun')
  })
})
