import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import DemoQuotaHint from '@/components/auth/DemoQuotaHint.vue'
import { useAuthStore } from '@/stores/auth'
import type { DemoQuotaResource } from '@/types/auth'

/** Logs in a demo account with the given remaining creations directly in the store. */
function logInDemoAccount(remainingProperties: number, remainingAppointments: number, remainingTenants = 10) {
  useAuthStore().currentUser = {
    username: 'demo-1',
    displayName: 'Demo',
    demoAccount: true,
    expiresAt: new Date(),
    remainingPropertyCreations: remainingProperties,
    remainingAppointmentCreations: remainingAppointments,
    remainingTenantCreations: remainingTenants,
    appointmentBufferMinutes: 15,
  }
}

/** Mounts the hint for the given resource. */
function mountHint(resource: DemoQuotaResource) {
  return mount(DemoQuotaHint, { props: { resource }, global: { plugins: [i18n] } })
}

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
})

describe('DemoQuotaHint', () => {
  it('tells a demo account how many more properties it may create', () => {
    logInDemoAccount(2, 3)

    expect(mountHint('properties').text()).toBe('Demo-Account: Sie können noch 2 weitere Objekte anlegen.')
  })

  it('tells a demo account how many more appointments it may create, in the singular for one', () => {
    logInDemoAccount(3, 1)

    expect(mountHint('appointments').text()).toBe('Demo-Account: Sie können noch einen weiteren Termin anlegen.')
  })

  it('tells a demo account how many more tenants it may create', () => {
    logInDemoAccount(3, 3, 7)

    expect(mountHint('tenants').text()).toBe('Demo-Account: Sie können noch 7 weitere Mieter anlegen.')
  })

  it('highlights an exhausted tenant limit', () => {
    logInDemoAccount(3, 3, 0)

    const wrapper = mountHint('tenants')

    expect(wrapper.text()).toContain('bereits alle 10 zusätzlichen Mieter angelegt')
    expect(wrapper.find('.demo-quota-hint').classes()).toContain('demo-quota-hint--exhausted')
  })

  it('highlights an exhausted limit', () => {
    logInDemoAccount(0, 3)

    const wrapper = mountHint('properties')

    expect(wrapper.text()).toContain('bereits alle 3 zusätzlichen Objekte angelegt')
    expect(wrapper.find('.demo-quota-hint').classes()).toContain('demo-quota-hint--exhausted')
  })

  it('shows nothing for a regular account', () => {
    useAuthStore().currentUser = {
      username: 'debschke',
      displayName: 'David Ebschke',
      demoAccount: false,
      expiresAt: null,
      remainingPropertyCreations: null,
      remainingAppointmentCreations: null,
      remainingTenantCreations: null,
      appointmentBufferMinutes: 15,
    }

    expect(mountHint('properties').find('.demo-quota-hint').exists()).toBe(false)
  })
})
