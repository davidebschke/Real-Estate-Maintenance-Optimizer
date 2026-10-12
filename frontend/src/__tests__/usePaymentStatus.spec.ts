import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { i18n } from '@/i18n'
import { MONTH_STATUSES, usePaymentStatus } from '@/composables/usePaymentStatus'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Mounts the composable inside a real component instance, since it depends on useI18n injection. */
function mountComposable() {
  let composable!: ReturnType<typeof usePaymentStatus>
  const Harness = defineComponent({
    setup() {
      composable = usePaymentStatus()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('usePaymentStatus', () => {
  it('labels every month status in German and English', () => {
    const { statusLabel } = mountComposable()

    expect(MONTH_STATUSES.map(statusLabel)).toEqual([
      'Bezahlt',
      'Teilweise bezahlt',
      'Noch offen',
      'Überfällig',
      'Folgt noch',
      'Nicht erfasst',
    ])

    i18n.global.locale.value = 'en'
    expect(MONTH_STATUSES.map(statusLabel)).toEqual([
      'Paid',
      'Partially paid',
      'Still open',
      'Overdue',
      'Upcoming',
      'Not tracked',
    ])
  })

  it('gives every month status an icon and a severity', () => {
    const { statusIcon, statusSeverity } = mountComposable()

    expect(statusIcon('PAID')).toBe('pi-check-circle')
    expect(statusSeverity('PAID')).toBe('success')
    expect(statusSeverity('PARTIAL')).toBe('warn')
    expect(statusSeverity('PENDING')).toBe('info')
    expect(statusSeverity('OVERDUE')).toBe('danger')
    expect(statusSeverity('UPCOMING')).toBe('muted')
    expect(statusSeverity('UNTRACKED')).toBe('muted')
    for (const status of MONTH_STATUSES) expect(statusIcon(status)).toMatch(/^pi-/)
  })

  it('labels the payment alerts and rates an overdue payment as more severe than a pending one', () => {
    const { alertLabel, alertSeverity } = mountComposable()

    expect(alertLabel('PENDING')).toBe('Zahlung offen')
    expect(alertLabel('OVERDUE')).toBe('Zahlung überfällig')
    expect(alertSeverity('PENDING')).toBe('warn')
    expect(alertSeverity('OVERDUE')).toBe('danger')
  })

  it('labels and rates how confidently a scanned payment was assigned', () => {
    const { matchLabel, matchSeverity } = mountComposable()

    expect(matchLabel('MATCH')).toBe('Sichere Zuordnung')
    expect(matchLabel('POSSIBLE')).toBe('Unsichere Zuordnung')
    expect(matchLabel('NONE')).toBe('Keine Zuordnung')
    expect(matchSeverity('MATCH')).toBe('success')
    expect(matchSeverity('POSSIBLE')).toBe('warn')
    expect(matchSeverity('NONE')).toBe('danger')
  })
})
