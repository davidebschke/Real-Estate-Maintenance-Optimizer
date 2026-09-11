import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import AppHeader from '@/components/layout/AppHeader.vue'
import App from '../App.vue'

describe('App', () => {
  it('renders the application header', () => {
    const wrapper = shallowMount(App)

    expect(wrapper.findComponent(AppHeader).exists()).toBe(true)
  })
})
