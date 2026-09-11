import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import App from '../App.vue'

/** Minimal router stub matching the production router's empty route table. */
const router = createRouter({ history: createWebHistory(), routes: [] })

describe('App', () => {
  it('renders the application header', () => {
    const wrapper = mount(App, {
      global: { plugins: [router, i18n, PrimeVue] },
    })
    expect(wrapper.find('header.app-header').exists()).toBe(true)
  })
})
