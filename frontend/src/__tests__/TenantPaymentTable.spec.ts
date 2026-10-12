import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import TenantPaymentTable from '@/components/payments/TenantPaymentTable.vue'
import { createMonths, createTenantPaymentYear } from '@/__tests__/paymentFixtures'
import type { TenantPaymentYear } from '@/types/payment'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Mounts the table with the given props, defaulting to 2026, one paid tenant and no loading state. */
function mountTable(props: Partial<{ year: number; tenants: TenantPaymentYear[]; isLoading: boolean }> = {}) {
  return mount(TenantPaymentTable, {
    props: { year: 2026, tenants: [createTenantPaymentYear()], isLoading: false, ...props },
    global: { plugins: [PrimeVue, i18n] },
  })
}

describe('TenantPaymentTable', () => {
  it('shows the year and a row per tenant with name, floor and monthly rent', () => {
    const wrapper = mountTable({
      tenants: [
        createTenantPaymentYear(),
        createTenantPaymentYear({ tenantId: 'tenant-2', firstName: 'Max', lastName: 'Meier', floor: 0, monthlyRent: 700 }),
      ],
    })

    expect(wrapper.find('.tenant-payment-table__year').text()).toBe('2026')
    const rows = wrapper.findAll('.tenant-payment-table__row')
    expect(rows).toHaveLength(2)
    expect(rows[0]!.find('.tenant-payment-table__tenant-name').text()).toBe('Erika Mustermann')
    expect(rows[0]!.find('.tenant-payment-table__tenant-meta').text()).toMatch(/^2\. Stockwerk · 950,00\s€$/)
    expect(rows[1]!.find('.tenant-payment-table__tenant-name').text()).toBe('Max Meier')
    expect(rows[1]!.find('.tenant-payment-table__tenant-meta').text()).toMatch(/^Erdgeschoss · 700,00\s€$/)
  })

  it('has twelve month columns plus the tenant column and twelve status cells per tenant', () => {
    const wrapper = mountTable()

    expect(wrapper.findAll('thead th')).toHaveLength(13)
    expect(wrapper.findAll('.tenant-payment-table__month-head')[9]!.text()).toBe('Okt')
    expect(wrapper.findAll('.tenant-payment-table__cell')).toHaveLength(12)
  })

  it('marks the status of every month with its label, including the amount of a partial payment', () => {
    const months = createMonths('PAID', {
      1: { status: 'UNTRACKED', paidAmount: 0 },
      3: { status: 'PARTIAL', paidAmount: 500 },
      4: { status: 'OVERDUE', paidAmount: 0 },
      10: { status: 'PENDING', paidAmount: 0 },
      11: { status: 'UPCOMING', paidAmount: 0 },
    })
    const wrapper = mountTable({ tenants: [createTenantPaymentYear({ months })] })

    const labels = wrapper
      .findAll('.tenant-payment-table__cell .payment-status-badge')
      .map((badge) => badge.attributes('aria-label'))
    expect(labels[0]).toBe('Nicht erfasst')
    expect(labels[1]).toMatch(/^Bezahlt: 950,00\s€$/)
    expect(labels[2]).toMatch(/^Teilweise bezahlt: 500,00\s€$/)
    expect(labels[3]).toBe('Überfällig')
    expect(labels[9]).toBe('Noch offen')
    expect(labels[10]).toBe('Folgt noch')
  })

  it('highlights a tenant with an alert and shows the alert badge', () => {
    const wrapper = mountTable({
      tenants: [
        createTenantPaymentYear({ alert: 'OVERDUE' }),
        createTenantPaymentYear({ tenantId: 'tenant-2', alert: 'NONE' }),
      ],
    })

    const rows = wrapper.findAll('.tenant-payment-table__row')
    expect(rows[0]!.classes()).toContain('tenant-payment-table__row--alert')
    expect(rows[0]!.find('.payment-alert-badge').text()).toBe('Zahlung überfällig')
    expect(rows[1]!.classes()).not.toContain('tenant-payment-table__row--alert')
    expect(rows[1]!.find('.payment-alert-badge').exists()).toBe(false)
  })

  it('asks for the previous and the next year', async () => {
    const wrapper = mountTable()

    await wrapper.find('.tenant-payment-table__previous').trigger('click')
    await wrapper.find('.tenant-payment-table__next').trigger('click')

    expect(wrapper.emitted('change-year')).toEqual([[2025], [2027]])
  })

  it('does not navigate beyond next year or before the year 2000', () => {
    const nextYear = new Date().getFullYear() + 1

    expect(mountTable({ year: nextYear }).find('.tenant-payment-table__next').attributes('disabled')).toBeDefined()
    expect(mountTable({ year: nextYear }).find('.tenant-payment-table__previous').attributes('disabled')).toBeUndefined()
    expect(mountTable({ year: 2000 }).find('.tenant-payment-table__previous').attributes('disabled')).toBeDefined()
  })

  it('lets the table scroll horizontally as a labelled, keyboard-reachable region', () => {
    const scroll = mountTable().find('.tenant-payment-table__scroll')

    expect(scroll.attributes('role')).toBe('region')
    expect(scroll.attributes('tabindex')).toBe('0')
    expect(scroll.attributes('aria-label')).toBe('Mietzahlungen 2026')
  })

  it('shows a loading hint while the first table is on its way', () => {
    const wrapper = mountTable({ tenants: [], isLoading: true })

    expect(wrapper.find('.tenant-payment-table__status').text()).toBe('Mietzahlungen werden geladen …')
    expect(wrapper.find('table').exists()).toBe(false)
    expect(wrapper.attributes('aria-busy')).toBe('true')
  })

  it('keeps showing the table while a newer one is loading', () => {
    const wrapper = mountTable({ isLoading: true })

    expect(wrapper.find('table').exists()).toBe(true)
    expect(wrapper.find('.tenant-payment-table__status').exists()).toBe(false)
  })

  it('shows a hint instead of the table when the property has no tenants', () => {
    const wrapper = mountTable({ tenants: [] })

    expect(wrapper.find('.tenant-payment-table__empty').text()).toBe('Für dieses Objekt sind noch keine Mieter hinterlegt.')
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('explains every status in a legend', () => {
    const wrapper = mountTable()

    expect(wrapper.findAll('.tenant-payment-table__legend-item').map((item) => item.text())).toEqual([
      'Bezahlt',
      'Teilweise bezahlt',
      'Noch offen',
      'Überfällig',
      'Folgt noch',
      'Nicht erfasst',
    ])
  })

  it('renders English texts and month names when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mountTable()

    expect(wrapper.findAll('.tenant-payment-table__month-head')[9]!.text()).toBe('Oct')
    expect(wrapper.find('.tenant-payment-table__tenant-head').text()).toBe('Tenant')
    expect(wrapper.find('.tenant-payment-table__previous').attributes('aria-label')).toBe('Previous year')
  })
})
