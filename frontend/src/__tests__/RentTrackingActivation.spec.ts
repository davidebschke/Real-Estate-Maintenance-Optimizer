import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import RentTrackingActivation from '@/components/payments/RentTrackingActivation.vue'
import { usePaymentsStore } from '@/stores/payments'
import * as paymentService from '@/services/paymentService'
import { httpError } from '@/__tests__/httpError'
import { createPaymentYearOverview } from '@/__tests__/paymentFixtures'

vi.mock('@/services/paymentService')

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  vi.mocked(paymentService.activateTracking).mockReset().mockResolvedValue({ trackingStart: '2026-01' })
  vi.mocked(paymentService.fetchPaymentYear).mockReset().mockResolvedValue(createPaymentYearOverview())
  vi.mocked(paymentService.fetchPaymentAlerts).mockReset().mockResolvedValue([])
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Mounts the component for a property, inactive by default. */
function mountActivation(trackingStart: string | null = null) {
  return mount(RentTrackingActivation, {
    props: { propertyId: 'property-1', trackingStart },
    global: { plugins: [PrimeVue, i18n] },
  })
}

describe('RentTrackingActivation', () => {
  it('explains the check and offers the activation while it is not activated', () => {
    const wrapper = mountActivation()

    expect(wrapper.find('.rent-tracking-activation__explanation').text()).toContain('nicht aktiviert')
    expect(wrapper.find('button').text()).toBe('Mietzahlungscheck aktivieren')
    expect(wrapper.find('.rent-tracking-activation__active').exists()).toBe(false)
  })

  it('shows since when the check is active, localized, and offers no activation', () => {
    const wrapper = mountActivation('2026-01')

    expect(wrapper.find('.rent-tracking-activation__active').text()).toBe('Aktiv seit 01/2026')
    expect(wrapper.find('button').exists()).toBe(false)
    expect(wrapper.find('.rent-tracking-activation__explanation').exists()).toBe(false)
  })

  it('renders the English texts when the locale is switched', () => {
    i18n.global.locale.value = 'en'

    expect(mountActivation().find('button').text()).toBe('Activate rent payment check')
    expect(mountActivation('2026-01').find('.rent-tracking-activation__active').text()).toBe('Active since 01/2026')
  })

  it('activates the check of the property when the button is clicked', async () => {
    const wrapper = mountActivation()

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(paymentService.activateTracking).toHaveBeenCalledWith('property-1')
    expect(paymentService.fetchPaymentYear).toHaveBeenCalledTimes(1)
  })

  it('disables the button and shows the activating state while the request is on its way', async () => {
    vi.mocked(paymentService.activateTracking).mockImplementationOnce(() => new Promise(() => {}))
    const wrapper = mountActivation()

    await wrapper.find('button').trigger('click')

    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    expect(wrapper.find('button').text()).toBe('Wird aktiviert …')
  })

  it('shows the backend message when activating fails', async () => {
    vi.mocked(paymentService.activateTracking).mockRejectedValue(httpError(404, { message: 'Objekt nicht gefunden.' }))
    const wrapper = mountActivation()

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(wrapper.find('.rent-tracking-activation__error').text()).toBe('Objekt nicht gefunden.')
    expect(wrapper.find('.rent-tracking-activation__error').attributes('role')).toBe('alert')
  })

  it('falls back to a generic text when the failure has neither a message nor a known status', async () => {
    vi.mocked(paymentService.activateTracking).mockRejectedValue(new Error('Network Error'))
    const wrapper = mountActivation()

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(wrapper.find('.rent-tracking-activation__error').text()).toBe('Der Mietzahlungscheck konnte nicht aktiviert werden.')
  })

  it('clears a previous failure when another activation starts and when it is unmounted', async () => {
    vi.mocked(paymentService.activateTracking).mockRejectedValueOnce(httpError(503))
    const wrapper = mountActivation()
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(usePaymentsStore().activationError).not.toBeNull()

    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(usePaymentsStore().activationError).toBeNull()

    vi.mocked(paymentService.activateTracking).mockRejectedValueOnce(httpError(503))
    await wrapper.find('button').trigger('click')
    await flushPromises()
    wrapper.unmount()
    expect(usePaymentsStore().activationError).toBeNull()
  })
})
