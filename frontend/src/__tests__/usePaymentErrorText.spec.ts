import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { i18n } from '@/i18n'
import { usePaymentErrorText } from '@/composables/usePaymentErrorText'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Mounts the composable inside a real component instance, since it depends on useI18n injection. */
function mountComposable() {
  let composable!: ReturnType<typeof usePaymentErrorText>
  const Harness = defineComponent({
    setup() {
      composable = usePaymentErrorText()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('usePaymentErrorText', () => {
  it('prefers the localized message the backend gave', () => {
    const { describeError } = mountComposable()

    expect(describeError({ status: 503, message: 'Die KI ist gerade nicht erreichbar.' }, 'payments.upload.error')).toBe(
      'Die KI ist gerade nicht erreichbar.',
    )
  })

  it.each([
    [400, 'Die Datei ist kein gültiges PDF oder enthält keine Zahlungen.'],
    [404, 'Das Objekt wurde nicht gefunden.'],
    [409, 'Der Mietzahlungscheck ist für dieses Objekt noch nicht aktiviert.'],
    [413, 'Die Datei ist zu groß. Erlaubt sind höchstens 5 MB.'],
    [429, 'Zu viele Anfragen. Bitte später erneut versuchen.'],
    [501, 'Das Einlesen von Kontoauszügen ist auf diesem Server nicht aktiviert.'],
    [503, 'Die KI ist gerade nicht verfügbar. Bitte später erneut versuchen.'],
  ])('falls back to a text for the status %i when the backend gave no message', (status, expected) => {
    const { describeError } = mountComposable()

    expect(describeError({ status, message: null }, 'payments.upload.error')).toBe(expected)
  })

  it('falls back to the generic text for an unknown status or a missing response', () => {
    const { describeError } = mountComposable()

    expect(describeError({ status: 500, message: null }, 'payments.upload.error')).toBe(
      'Der Kontoauszug konnte nicht eingelesen werden.',
    )
    expect(describeError({ status: null, message: null }, 'payments.preview.saveError')).toBe(
      'Die Zahlungen konnten nicht gespeichert werden.',
    )
  })

  it('renders the English fallback when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const { describeError } = mountComposable()

    expect(describeError({ status: 413, message: null }, 'payments.upload.error')).toBe(
      'The file is too large. At most 5 MB are allowed.',
    )
  })
})
