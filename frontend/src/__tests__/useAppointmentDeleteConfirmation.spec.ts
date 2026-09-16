import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import { useAppointmentDeleteConfirmation } from '@/composables/useAppointmentDeleteConfirmation'
import type { Appointment } from '@/types/appointment'
import type { AppointmentDeleteScope } from '@/services/appointmentService'

vi.mock('primevue/useconfirm')

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Kellerreinigung Q3',
    propertyId: 'property-1',
    propertyName: 'Wohnanlage Sonnenhof',
    propertyAddress: 'Aachener Str. 512',
    description: '',
    category: 'maintenance',
    start: new Date(2026, 7, 11, 13, 0),
    end: new Date(2026, 7, 11, 15, 0),
    locked: false,
    recurring: false,
    recurrenceIntervalMonths: null,
    materials: [],
    history: [],
    travelDistanceKm: 0,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

/** Mounts the composable inside a real component instance, since it depends on useI18n/useConfirm injection. */
function mountComposable() {
  let composable!: ReturnType<typeof useAppointmentDeleteConfirmation>
  const Harness = defineComponent({
    setup() {
      composable = useAppointmentDeleteConfirmation()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('useAppointmentDeleteConfirmation', () => {
  it('asks a plain yes/no confirmation for a non-recurring appointment', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    const { confirmDelete } = mountComposable()
    const onConfirmed = vi.fn<(scope: AppointmentDeleteScope) => void>()
    confirmDelete(createAppointment({ recurring: false }), onConfirmed)

    const options = require.mock.calls[0]![0] as { accept: () => void }
    options.accept()

    expect(onConfirmed).toHaveBeenCalledWith('single')
  })

  it('offers a series-vs-single choice for a recurring appointment', () => {
    const require = vi.fn<(options: Record<string, unknown>) => void>()
    vi.mocked(useConfirm).mockReturnValue({ require } as never)

    const { confirmDelete } = mountComposable()
    const onConfirmed = vi.fn<(scope: AppointmentDeleteScope) => void>()
    confirmDelete(createAppointment({ recurring: true }), onConfirmed)

    const options = require.mock.calls[0]![0] as { accept: () => void; reject: () => void }
    options.accept()
    expect(onConfirmed).toHaveBeenCalledWith('series')

    onConfirmed.mockReset()
    options.reject()
    expect(onConfirmed).toHaveBeenCalledWith('single')
  })
})
