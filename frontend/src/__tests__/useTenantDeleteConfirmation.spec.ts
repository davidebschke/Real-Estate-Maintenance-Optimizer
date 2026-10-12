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

    mountComposable().confirmDelete(tenant, vi.fn())

    expect(require).toHaveBeenCalledOnce()
    const options = require.mock.calls[0]![0]
    expect(options.header).toBe('Mieter löschen')
    expect(options.message).toContain('Soll der Mieter „Erika Mustermann“ wirklich gelöscht werden?')
  })

  it('notes that the rent payments of the tenant are kept and never mentions deleting the apartment', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    mountComposable().confirmDelete(tenant, vi.fn())

    const message = require.mock.calls[0]![0].message as string
    expect(message).toContain('Zahlungen des vorherigen Mieters erhalten')
    expect(message).not.toContain('Wohnungsdaten')
  })

  it('invokes the callback only once the user accepts', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)
    const onConfirmed = vi.fn()

    mountComposable().confirmDelete(tenant, onConfirmed)
    expect(onConfirmed).not.toHaveBeenCalled()

    ;(require.mock.calls[0]![0].accept as () => void)()
    expect(onConfirmed).toHaveBeenCalledOnce()
  })
})
