import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import PaymentUploadButton from '@/components/payments/PaymentUploadButton.vue'
import { usePaymentsStore } from '@/stores/payments'
import * as paymentService from '@/services/paymentService'
import { httpError } from '@/__tests__/httpError'
import { createScannedPayment } from '@/__tests__/paymentFixtures'
import { MAX_STATEMENT_BYTES } from '@/utils/statementFileValidation'

vi.mock('@/services/paymentService')

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  vi.mocked(paymentService.scanStatement).mockReset().mockResolvedValue([createScannedPayment()])
})

afterEach(() => {
  vi.restoreAllMocks()
})

/** Builds a file of the given size, name and MIME type. */
function createFile(size = 1024, name = 'statement.pdf', type = 'application/pdf'): File {
  return new File([new Uint8Array(size)], name, { type })
}

/** Mounts the upload button for a property. */
function mountButton() {
  return mount(PaymentUploadButton, {
    props: { propertyId: 'property-1' },
    global: { plugins: [PrimeVue, i18n] },
  })
}

/** Simulates choosing the given file in the hidden file input. */
async function chooseFile(wrapper: ReturnType<typeof mountButton>, file: File) {
  const input = wrapper.find<HTMLInputElement>('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
  await flushPromises()
}

describe('PaymentUploadButton', () => {
  it('offers a button with an upload icon and a hidden PDF-only file input', () => {
    const wrapper = mountButton()

    const input = wrapper.find('input[type="file"]')
    expect(input.attributes('accept')).toBe('application/pdf')
    expect(input.attributes('hidden')).toBeDefined()
    expect(wrapper.find('button').text()).toBe('Kontoauszug einlesen')
    expect(wrapper.find('button .pi-upload').exists()).toBe(true)
    expect(wrapper.find('.payment-upload-button__hint').text()).toContain('höchstens 5 MB')
  })

  it('opens the file picker when the button is clicked', async () => {
    const click = vi.spyOn(HTMLInputElement.prototype, 'click').mockImplementation(() => {})
    const wrapper = mountButton()

    await wrapper.find('button').trigger('click')

    expect(click).toHaveBeenCalledTimes(1)
  })

  it('sends a valid PDF to be scanned for the property', async () => {
    const wrapper = mountButton()
    const file = createFile()

    await chooseFile(wrapper, file)

    expect(paymentService.scanStatement).toHaveBeenCalledWith('property-1', file)
    expect(usePaymentsStore().scanSession?.propertyId).toBe('property-1')
    expect(wrapper.find('.payment-upload-button__error').exists()).toBe(false)
  })

  it('shows the loading state and disables the button while the statement is scanned', async () => {
    let resolveScan!: (payments: ReturnType<typeof createScannedPayment>[]) => void
    vi.mocked(paymentService.scanStatement).mockImplementationOnce(() => new Promise((resolve) => (resolveScan = resolve)))
    const wrapper = mountButton()

    const pending = chooseFile(wrapper, createFile())
    await wrapper.vm.$nextTick()

    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    expect(wrapper.find('button').text()).toBe('Kontoauszug wird gelesen …')
    expect(wrapper.find('.payment-upload-button__status').exists()).toBe(true)

    resolveScan([])
    await pending
    await flushPromises()

    expect(wrapper.find('button').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('.payment-upload-button__status').exists()).toBe(false)
  })

  it.each([
    ['a file that is not a PDF', createFile(10, 'photo.png', 'image/png'), 'Bitte eine PDF-Datei auswählen.'],
    ['an empty file', createFile(0), 'Die Datei ist leer.'],
    ['a file above 5 MB', createFile(MAX_STATEMENT_BYTES + 1), 'Die Datei ist größer als 5 MB.'],
  ])('rejects %s without sending it', async (_description, file, message) => {
    const wrapper = mountButton()

    await chooseFile(wrapper, file)

    expect(paymentService.scanStatement).not.toHaveBeenCalled()
    expect(wrapper.find('.payment-upload-button__error').text()).toBe(message)
    expect(wrapper.find('.payment-upload-button__error').attributes('role')).toBe('alert')
  })

  it('does nothing when the file picker was closed without a choice', async () => {
    const wrapper = mountButton()
    const input = wrapper.find<HTMLInputElement>('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [], configurable: true })

    await input.trigger('change')

    expect(paymentService.scanStatement).not.toHaveBeenCalled()
  })

  it('shows the message the backend gave when the scan fails', async () => {
    vi.mocked(paymentService.scanStatement).mockRejectedValue(
      httpError(503, { message: 'Die KI ist gerade nicht erreichbar.' }),
    )
    const wrapper = mountButton()

    await chooseFile(wrapper, createFile())

    expect(wrapper.find('.payment-upload-button__error').text()).toBe('Die KI ist gerade nicht erreichbar.')
    expect(usePaymentsStore().scanSession).toBeNull()
  })

  it.each([
    [400, 'Die Datei ist kein gültiges PDF oder enthält keine Zahlungen.'],
    [413, 'Die Datei ist zu groß. Erlaubt sind höchstens 5 MB.'],
    [429, 'Zu viele Anfragen. Bitte später erneut versuchen.'],
    [501, 'Das Einlesen von Kontoauszügen ist auf diesem Server nicht aktiviert.'],
    [503, 'Die KI ist gerade nicht verfügbar. Bitte später erneut versuchen.'],
  ])('shows a localized text for status %i when the backend sent no message', async (status, message) => {
    vi.mocked(paymentService.scanStatement).mockRejectedValue(httpError(status))
    const wrapper = mountButton()

    await chooseFile(wrapper, createFile())

    expect(wrapper.find('.payment-upload-button__error').text()).toBe(message)
  })

  it('shows a generic text for a failure without a response', async () => {
    vi.mocked(paymentService.scanStatement).mockRejectedValue(new Error('Network Error'))
    const wrapper = mountButton()

    await chooseFile(wrapper, createFile())

    expect(wrapper.find('.payment-upload-button__error').text()).toBe('Der Kontoauszug konnte nicht eingelesen werden.')
  })

  it('removes an earlier problem when a valid file is chosen afterwards', async () => {
    const wrapper = mountButton()
    await chooseFile(wrapper, createFile(0))
    expect(wrapper.find('.payment-upload-button__error').exists()).toBe(true)

    await chooseFile(wrapper, createFile())

    expect(wrapper.find('.payment-upload-button__error').exists()).toBe(false)
  })

  it('forgets a scan error when it is removed', async () => {
    vi.mocked(paymentService.scanStatement).mockRejectedValue(httpError(503))
    const wrapper = mountButton()
    await chooseFile(wrapper, createFile())
    expect(usePaymentsStore().scanError).not.toBeNull()

    wrapper.unmount()

    expect(usePaymentsStore().scanError).toBeNull()
  })

  it('renders English texts when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mountButton()

    expect(wrapper.find('button').text()).toBe('Read bank statement')
  })
})
