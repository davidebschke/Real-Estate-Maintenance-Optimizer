import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import { useTenantDeleteConfirmation } from '@/composables/useTenantDeleteConfirmation'

vi.mock('primevue/useconfirm')

const tenant = { id: 'tenant-1', firstName: 'Erika', lastName: 'Mustermann' }

/** Mounts the composable inside a real component instance, since it depends on useI18n/useConfirm injection. */
function mountComposable() {
  let composable!: ReturnType<typeof useTenantDeleteConfirmation>
  const Harness = defineComponent({
    setup() {
      composable = useTenantDeleteConfirmation()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('useTenantDeleteConfirmation', () => {
  it('asks a plain yes/no confirmation naming the tenant', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    mountComposable().confirmDelete(tenant, false, vi.fn())

    expect(require).toHaveBeenCalledOnce()
    const options = require.mock.calls[0]![0]
    expect(options.header).toBe('Mieter löschen')
    expect(options.message).toBe('Soll der Mieter „Erika Mustermann“ wirklich gelöscht werden?')
  })

  it('warns that the apartment details are deleted too when it is the last tenant', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    mountComposable().confirmDelete(tenant, true, vi.fn())

    expect(require.mock.calls[0]![0].message).toContain('werden auch die Wohnungsdaten gelöscht')
  })

  it('invokes the callback only once the user accepts', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)
    const onConfirmed = vi.fn()

    mountComposable().confirmDelete(tenant, false, onConfirmed)
    expect(onConfirmed).not.toHaveBeenCalled()

    ;(require.mock.calls[0]![0].accept as () => void)()
    expect(onConfirmed).toHaveBeenCalledOnce()
  })
})
