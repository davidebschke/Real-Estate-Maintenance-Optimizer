import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import PaymentAlertBadge from '@/components/payments/PaymentAlertBadge.vue'
import type { PaymentAlert } from '@/types/payment'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Mounts the badge for the given alert. */
function mountBadge(alert: PaymentAlert, compact = false) {
  return mount(PaymentAlertBadge, { props: { alert, compact }, global: { plugins: [i18n] } })
}

describe('PaymentAlertBadge', () => {
  it('renders nothing when there is no alert', () => {
    expect(mountBadge('NONE').find('.payment-alert-badge').exists()).toBe(false)
  })

  it('shows a pending payment as a warning with its label', () => {
    const wrapper = mountBadge('PENDING')

    expect(wrapper.find('.payment-alert-badge__label').text()).toBe('Zahlung offen')
    expect(wrapper.classes()).toContain('payment-severity--warn')
    expect(wrapper.attributes('role')).toBe('status')
  })

  it('shows an overdue payment as a danger with its label', () => {
    const wrapper = mountBadge('OVERDUE')

    expect(wrapper.find('.payment-alert-badge__label').text()).toBe('Zahlung überfällig')
    expect(wrapper.classes()).toContain('payment-severity--danger')
  })

  it('shows only the icon in the compact variant but keeps the accessible name', () => {
    const wrapper = mountBadge('OVERDUE', true)

    expect(wrapper.find('.payment-alert-badge__label').exists()).toBe(false)
    expect(wrapper.classes()).toContain('payment-alert-badge--compact')
    expect(wrapper.attributes('aria-label')).toBe('Zahlung überfällig')
    expect(wrapper.attributes('title')).toBe('Zahlung überfällig')
  })

  it('renders the English label when the locale is switched', () => {
    i18n.global.locale.value = 'en'

    expect(mountBadge('PENDING').find('.payment-alert-badge__label').text()).toBe('Payment pending')
  })
})
