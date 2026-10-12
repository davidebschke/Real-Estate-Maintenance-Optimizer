import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import PaymentStatusBadge from '@/components/payments/PaymentStatusBadge.vue'
import { MONTH_STATUSES } from '@/composables/usePaymentStatus'
import type { MonthPaymentStatus } from '@/types/payment'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Mounts the badge for the given status and optional detail. */
function mountBadge(status: MonthPaymentStatus, detail?: string) {
  return mount(PaymentStatusBadge, { props: { status, detail }, global: { plugins: [i18n] } })
}

describe('PaymentStatusBadge', () => {
  it('draws the status as an icon with the localized label as accessible name and tooltip', () => {
    const wrapper = mountBadge('PAID')

    expect(wrapper.attributes('role')).toBe('img')
    expect(wrapper.attributes('aria-label')).toBe('Bezahlt')
    expect(wrapper.attributes('title')).toBe('Bezahlt')
    expect(wrapper.find('i').classes()).toContain('pi-check-circle')
    expect(wrapper.find('i').attributes('aria-hidden')).toBe('true')
  })

  it('appends the detail to the label', () => {
    const wrapper = mountBadge('PARTIAL', '500,00 €')

    expect(wrapper.attributes('aria-label')).toBe('Teilweise bezahlt: 500,00 €')
  })

  it.each([
    ['PAID', 'success'],
    ['PARTIAL', 'warn'],
    ['PENDING', 'info'],
    ['OVERDUE', 'danger'],
    ['UPCOMING', 'muted'],
    ['UNTRACKED', 'muted'],
  ] as const)('colors %s with the %s severity', (status, severity) => {
    expect(mountBadge(status).classes()).toContain(`payment-severity--${severity}`)
  })

  it('renders every status with a distinct label', () => {
    const labels = MONTH_STATUSES.map((status) => mountBadge(status).attributes('aria-label'))

    expect(new Set(labels).size).toBe(MONTH_STATUSES.length)
  })

  it('renders the English label when the locale is switched', () => {
    i18n.global.locale.value = 'en'

    expect(mountBadge('OVERDUE').attributes('aria-label')).toBe('Overdue')
  })
})
