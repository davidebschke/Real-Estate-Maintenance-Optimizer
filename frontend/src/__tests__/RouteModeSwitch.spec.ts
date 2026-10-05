import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import RouteModeSwitch from '@/components/map/RouteModeSwitch.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('RouteModeSwitch', () => {
  it('offers the car and walking modes and marks the selected one as active', () => {
    const wrapper = mount(RouteModeSwitch, {
      props: { modelValue: 'walking' },
      global: { plugins: [i18n] },
    })

    const buttons = wrapper.findAll('.route-mode-switch__button')
    expect(buttons.map((button) => button.text())).toEqual(['PKW', 'Zu Fuß'])
    expect(buttons[0]?.attributes('aria-pressed')).toBe('false')
    expect(buttons[1]?.attributes('aria-pressed')).toBe('true')
    expect(buttons[1]?.classes()).toContain('route-mode-switch__button--active')
  })

  it('emits the clicked mode', async () => {
    const wrapper = mount(RouteModeSwitch, {
      props: { modelValue: 'car' },
      global: { plugins: [i18n] },
    })

    await wrapper.findAll('.route-mode-switch__button')[1]?.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([['walking']])
  })

  it('labels the modes in English when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(RouteModeSwitch, {
      props: { modelValue: 'car' },
      global: { plugins: [i18n] },
    })

    expect(wrapper.findAll('.route-mode-switch__button').map((button) => button.text())).toEqual([
      'Car',
      'On foot',
    ])
  })
})
