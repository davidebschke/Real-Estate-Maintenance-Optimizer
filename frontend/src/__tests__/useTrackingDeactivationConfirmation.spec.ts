import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import { useTrackingDeactivationConfirmation } from '@/composables/useTrackingDeactivationConfirmation'

vi.mock('primevue/useconfirm')

/** Mounts the composable inside a real component instance, since it depends on useI18n/useConfirm injection. */
function mountComposable() {
  let composable!: ReturnType<typeof useTrackingDeactivationConfirmation>
  const Harness = defineComponent({
    setup() {
      composable = useTrackingDeactivationConfirmation()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('useTrackingDeactivationConfirmation', () => {
  it('asks a plain yes/no confirmation explaining that no payments are checked until reactivation', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    const { confirmDeactivation } = mountComposable()
    const onConfirmed = vi.fn<() => void>()
    confirmDeactivation(onConfirmed)

    expect(require).toHaveBeenCalledTimes(1)
    const options = require.mock.calls[0]![0] as { header: string; message: string; accept: () => void }
    expect(options.header).toBe('Mietzahlungscheck deaktivieren')
    expect(options.message).toContain('keine Mietzahlungen mehr geprüft')
    expect(onConfirmed).not.toHaveBeenCalled()

    options.accept()

    expect(onConfirmed).toHaveBeenCalledTimes(1)
  })
})
