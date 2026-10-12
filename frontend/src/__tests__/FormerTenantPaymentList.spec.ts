import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import FormerTenantPaymentList from '@/components/payments/FormerTenantPaymentList.vue'
import { createFormerTenantPayment } from '@/__tests__/paymentFixtures'
import type { FormerTenantPayment } from '@/types/payment'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Mounts the list with the given payments, defaulting to one payment of a former tenant. */
function mountList(payments: FormerTenantPayment[] = [createFormerTenantPayment()]) {
  return mount(FormerTenantPaymentList, { props: { payments }, global: { plugins: [i18n] } })
}

describe('FormerTenantPaymentList', () => {
  it('shows the heading and a row per payment', () => {
    const wrapper = mountList([
      createFormerTenantPayment(),
      createFormerTenantPayment({ apartmentId: 'apartment-2', floor: 0, periodMonth: 4, paymentDate: '2026-04-03' }),
    ])

    expect(wrapper.find('.former-tenant-payment-list__heading').text()).toBe('Zahlungen früherer Mieter')
    expect(wrapper.findAll('.former-tenant-payment-list__row')).toHaveLength(2)
  })

  it('shows date, amount, payment month, apartment floor and the note of the previous tenant', () => {
    const row = mountList().find('.former-tenant-payment-list__row')

    expect(row.find('.former-tenant-payment-list__date').text()).toContain('02.03.2026')
    expect(row.find('.former-tenant-payment-list__amount').text()).toMatch(/950,00\s€/)
    expect(row.find('.former-tenant-payment-list__period').text()).toContain('März 2026')
    expect(row.find('.former-tenant-payment-list__apartment').text()).toContain('2. Stockwerk')
    expect(row.find('.former-tenant-payment-list__note').text()).toContain('Zahlung des vorherigen Mieters Mustermann, Erika')
  })

  it('keeps the column headers and labels every cell with its column name for the card layout', () => {
    const wrapper = mountList()
    const columns = ['Datum', 'Betrag', 'Zahlungsmonat', 'Wohnung', 'Vermerk']

    expect(wrapper.findAll('thead th').map((header) => header.text())).toEqual(columns)
    expect(wrapper.findAll('tbody td').map((cell) => cell.attributes('data-label'))).toEqual(columns)
  })

  it('renders English texts and month names when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mountList()

    expect(wrapper.find('.former-tenant-payment-list__heading').text()).toBe('Payments of former tenants')
    expect(wrapper.find('.former-tenant-payment-list__period').text()).toContain('March 2026')
    expect(wrapper.find('.former-tenant-payment-list__note').text()).toContain('Payment of the previous tenant Mustermann, Erika')
  })
})
