import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { i18n } from '@/i18n'
import { useDurationOptions } from '@/composables/useDurationOptions'
import { DURATION_OPTIONS } from '@/utils/appointmentSchedulingOptions'

/** Mounts the composable inside a real component instance, since it depends on useI18n injection. */
function mountComposable() {
  let composable!: ReturnType<typeof useDurationOptions>
  const Harness = defineComponent({
    setup() {
      composable = useDurationOptions()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('useDurationOptions', () => {
  it('offers every duration option with its minutes and a translated label', () => {
    const { durationOptions } = mountComposable()

    expect(durationOptions.value).toHaveLength(DURATION_OPTIONS.length)
    expect(durationOptions.value[0]).toMatchObject({ key: 'oneHour', minutes: 60 })
    expect(durationOptions.value[0]!.label).not.toContain('appointments.form.duration')
  })
})
