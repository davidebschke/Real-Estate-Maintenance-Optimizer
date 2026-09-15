import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import App from '../App.vue'

describe('App', () => {
  it('renders the application header and footer', () => {
    const wrapper = shallowMount(App, {
      global: { plugins: [createPinia()] },
    })

    expect(wrapper.findComponent(AppHeader).exists()).toBe(true)
    expect(wrapper.findComponent(AppFooter).exists()).toBe(true)
  })
})
