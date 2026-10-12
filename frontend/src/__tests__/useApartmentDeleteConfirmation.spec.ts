import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import { useApartmentDeleteConfirmation } from '@/composables/useApartmentDeleteConfirmation'
import type { Apartment } from '@/types/apartment'

vi.mock('primevue/useconfirm')

const apartment: Apartment = {
  id: 'apartment-1',
  propertyId: 'property-1',
  floor: 2,
  areaSquareMeters: 64.5,
  totalRent: 850,
  coldRent: 650,
  additionalCosts: 200,
  tenants: [],
}

/** Mounts the composable inside a real component instance, since it depends on useI18n/useConfirm injection. */
function mountComposable() {
  let composable!: ReturnType<typeof useApartmentDeleteConfirmation>
  const Harness = defineComponent({
    setup() {
      composable = useApartmentDeleteConfirmation()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('useApartmentDeleteConfirmation', () => {
  it('names the apartment by floor and area in the confirmation', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    mountComposable().confirmDelete(apartment, vi.fn())

    expect(require).toHaveBeenCalledOnce()
    const options = require.mock.calls[0]![0]
    expect(options.header).toBe('Wohnung löschen')
    expect(options.message).toContain('Soll die Wohnung „2. Stockwerk, 64,5 m²“ wirklich gelöscht werden?')
  })

  it('warns that tenants, their rent payments and the rent history are deleted too and that this cannot be undone', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    mountComposable().confirmDelete(apartment, vi.fn())

    const message = require.mock.calls[0]![0].message as string
    expect(message).toContain('Ihre Mieter, deren Mietzahlungen und der Mietverlauf werden mitgelöscht')
    expect(message).toContain('nicht rückgängig')
  })

  it('invokes the callback only once the user accepts', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)
    const onConfirmed = vi.fn()

    mountComposable().confirmDelete(apartment, onConfirmed)
    expect(onConfirmed).not.toHaveBeenCalled()

    ;(require.mock.calls[0]![0].accept as () => void)()
    expect(onConfirmed).toHaveBeenCalledOnce()
  })
})
