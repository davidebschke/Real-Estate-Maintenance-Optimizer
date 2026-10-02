import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'
import { useToast } from 'primevue/usetoast'
import { i18n } from '@/i18n'
import AppCalendar from '@/components/calendar/AppCalendar.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { useAppointmentDragAndDrop } from '@/composables/useAppointmentDragAndDrop'
import * as appointmentService from '@/services/appointmentService'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/appointmentService')
vi.mock('primevue/usetoast')
vi.mock('@/composables/useAppointmentDragAndDrop')

type DropHandler = (appointmentId: string, newStart: Date, durationMinutes: number) => Promise<void>

/** Builds a sample appointment for tests. */
function createAppointment(): Appointment {
  return {
    id: 'a1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: 'property-1',
    propertyName: 'Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: '',
    category: 'maintenance',
    start: new Date(2026, 9, 7, 9, 0),
    end: new Date(2026, 9, 7, 10, 0),
    locked: false,
    recurring: false,
    recurrenceIntervalMonths: null,
    materials: [],
    history: [],
    travelDistanceKm: 0,
    actualEnd: null,
    completed: false,
  }
}

/** Mounts the calendar and returns the drop handler it handed to the drag-and-drop composable. */
async function mountCalendarAndCaptureDropHandler() {
  let onDrop!: DropHandler
  vi.mocked(useAppointmentDragAndDrop).mockImplementation((_grid, _view, _range, handler) => {
    onDrop = handler as DropHandler
    return {
      preview: reactive({ isDragging: false, isValidDrop: true, label: '', pointerX: 0, pointerY: 0 }),
      handlePointerDown: vi.fn(),
      wasLastInteractionADrag: vi.fn(() => false),
    }
  })
  mount(AppCalendar, { global: { plugins: [i18n, createPinia()] } })
  await flushPromises()
  return onDrop
}

describe('AppCalendar drag-and-drop move conflicts', () => {
  const add = vi.fn<(message: Record<string, unknown>) => void>()

  beforeEach(() => {
    setActivePinia(createPinia())
    add.mockReset()
    vi.mocked(useToast).mockReturnValue({ add } as never)
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([createAppointment()])
    vi.mocked(appointmentService.moveAppointment).mockReset()
  })

  it('shows a toast with the conflict message and suggested slot and clears the conflict when the drop is blocked', async () => {
    vi.mocked(appointmentService.moveAppointment).mockResolvedValue({
      status: 'conflict',
      message: 'Der gewählte Zeitraum überschneidet sich mit einem bereits bestehenden Termin.',
      suggestedStart: new Date(2026, 9, 7, 15, 15),
      suggestedEnd: new Date(2026, 9, 7, 16, 15),
    })
    const onDrop = await mountCalendarAndCaptureDropHandler()

    await onDrop('a1', new Date(2026, 9, 7, 14, 0), 60)

    expect(add).toHaveBeenCalledTimes(1)
    const toast = add.mock.calls[0]![0]
    expect(toast.severity).toBe('warn')
    expect(toast.detail).toContain('überschneidet sich mit einem bereits bestehenden Termin')
    expect(toast.detail).toContain('15:15')
    expect(useAppointmentsStore().appointmentConflict).toBeNull()
  })

  it('shows no toast when the drop moves the appointment', async () => {
    vi.mocked(appointmentService.moveAppointment).mockResolvedValue({
      status: 'moved',
      appointment: createAppointment(),
    })
    const onDrop = await mountCalendarAndCaptureDropHandler()

    await onDrop('a1', new Date(2026, 9, 7, 14, 0), 60)

    expect(add).not.toHaveBeenCalled()
  })
})
