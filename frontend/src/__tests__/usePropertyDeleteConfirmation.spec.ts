import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import { usePropertyDeleteConfirmation } from '@/composables/usePropertyDeleteConfirmation'
import type { Property } from '@/types/property'

vi.mock('primevue/useconfirm')

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: '1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
    latitude: 50.94,
    longitude: 6.88,
    ...overrides,
  }
}

/** Mounts the composable inside a real component instance, since it depends on useI18n/useConfirm injection. */
function mountComposable() {
  let composable!: ReturnType<typeof usePropertyDeleteConfirmation>
  const Harness = defineComponent({
    setup() {
      composable = usePropertyDeleteConfirmation()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('usePropertyDeleteConfirmation', () => {
  it('asks a plain yes/no confirmation warning about cascading appointment deletion', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    const { confirmDelete } = mountComposable()
    const onConfirmed = vi.fn<() => void>()
    confirmDelete(createProperty(), onConfirmed)

    expect(require).toHaveBeenCalledTimes(1)
    const options = require.mock.calls[0]![0] as { message: string; accept: () => void }
    expect(options.message).toContain('Wohnanlage Sonnenhof')

    options.accept()

    expect(onConfirmed).toHaveBeenCalled()
  })
})
