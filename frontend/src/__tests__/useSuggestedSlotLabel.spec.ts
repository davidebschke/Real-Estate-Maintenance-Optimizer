import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { i18n } from '@/i18n'
import { useSuggestedSlotLabel } from '@/composables/useSuggestedSlotLabel'
import type { AppointmentConflict } from '@/services/appointmentService'

const conflict: AppointmentConflict = {
  message: 'Konflikt',
  suggestedStart: new Date(2026, 9, 15, 15, 15),
  suggestedEnd: new Date(2026, 9, 15, 16, 15),
}

/** Mounts the composable inside a real component instance, since it depends on useI18n injection. */
function mountComposable() {
  let composable!: ReturnType<typeof useSuggestedSlotLabel>
  const Harness = defineComponent({
    setup() {
      composable = useSuggestedSlotLabel()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('useSuggestedSlotLabel', () => {
  afterEach(() => {
    i18n.global.locale.value = 'de'
  })

  it('formats the suggested start as a day label followed by the time', () => {
    const { formatSuggestedSlot } = mountComposable()

    expect(formatSuggestedSlot(conflict)).toMatch(/15\.10\..*, 15:15/)
  })

  it('follows the active locale', () => {
    i18n.global.locale.value = 'en'
    const { formatSuggestedSlot } = mountComposable()

    expect(formatSuggestedSlot(conflict)).toContain('3:15')
  })
})
