import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Select from 'primevue/select'
import Button from 'primevue/button'
import { useConfirm } from 'primevue/useconfirm'
import { i18n } from '@/i18n'
import AppointmentDetailDrawer from '@/components/appointments/AppointmentDetailDrawer.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import * as appointmentService from '@/services/appointmentService'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/appointmentService')
vi.mock('primevue/useconfirm')

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Kellerreinigung Q3',
    propertyId: 'property-1',
    propertyName: 'Wohnanlage Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: 'Was ist zu tun?',
    category: 'maintenance',
    start: new Date(2026, 7, 11, 13, 0),
    end: new Date(2026, 7, 11, 15, 0),
    locked: false,
    recurring: false,
    recurrenceIntervalMonths: null,
    materials: ['Kehrmaschine'],
    history: [{ timestamp: new Date(2026, 7, 1, 10, 0), message: 'Termin erstellt.' }],
    travelDistanceKm: 0,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
  vi.mocked(appointmentService.moveAppointment).mockReset()
  vi.mocked(appointmentService.completeAppointment).mockReset()
  vi.mocked(appointmentService.reopenAppointment).mockReset()
  vi.mocked(appointmentService.deleteAppointment).mockReset()
  document.body.innerHTML = ''
})

/** Mounts the drawer already open for the given appointment; PrimeVue's Drawer teleports its content one microtask later. */
async function mountDrawer(appointment: Appointment) {
  const store = useAppointmentsStore()
  store.appointments = [appointment]

  const wrapper = mount(AppointmentDetailDrawer, {
    props: { visible: true, appointmentId: appointment.id },
    global: { plugins: [i18n, PrimeVue] },
    attachTo: document.body,
  })
  await flushPromises()
  return { wrapper, store }
}

describe('AppointmentDetailDrawer', () => {
  it("renders the appointment's title, property, description and materials", async () => {
    await mountDrawer(createAppointment())

    expect(document.body.querySelector('.appointment-detail-drawer__title')?.textContent).toBe(
      'Kellerreinigung Q3',
    )
    expect(document.body.textContent).toContain('Wohnanlage Sonnenhof')
    expect(document.body.textContent).toContain('Was ist zu tun?')
    expect(document.body.textContent).toContain('Kehrmaschine')
  })

  it('shows a locked hint instead of a move button for an unverschiebbar appointment', async () => {
    const { wrapper } = await mountDrawer(createAppointment({ locked: true }))

    expect(document.body.querySelector('.appointment-detail-drawer__locked-hint')).not.toBeNull()
    expect(
      wrapper.findAllComponents(Button).some((button) => button.text() === 'Verschieben'),
    ).toBe(false)
  })

  it('reschedules an unlocked appointment via the schedule form', async () => {
    vi.mocked(appointmentService.moveAppointment).mockResolvedValue(createAppointment())
    const { wrapper } = await mountDrawer(createAppointment())

    const moveButton = wrapper
      .findAllComponents(Button)
      .find((button) => button.text() === 'Verschieben')
    await moveButton!.trigger('click')

    const selects = wrapper.findAllComponents(Select)
    await selects[0]!.vm.$emit('update:modelValue', '2026-08-12')
    await selects[1]!.vm.$emit('update:modelValue', '09:00')
    await selects[2]!.vm.$emit('update:modelValue', 60)

    const saveButton = wrapper
      .findAllComponents(Button)
      .find((button) => button.text() === 'Speichern')
    await saveButton!.trigger('click')
    await flushPromises()

    expect(appointmentService.moveAppointment).toHaveBeenCalledWith(
      '1',
      expect.objectContaining({ start: new Date(2026, 7, 12, 9, 0), durationMinutes: 60 }),
    )
  })

  it('opens a date/time form pre-filled with the current time and marks the appointment as completed on confirm', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 7, 11, 16, 45))
    vi.mocked(appointmentService.completeAppointment).mockResolvedValue(
      createAppointment({ completed: true, actualEnd: new Date(2026, 7, 11, 14, 30) }),
    )
    const { wrapper } = await mountDrawer(createAppointment())

    const completeButton = wrapper
      .findAllComponents(Button)
      .find((button) => button.text() === 'Als erledigt markieren')
    await completeButton!.trigger('click')

    const dateInput = document.body.querySelector<HTMLInputElement>('#appointment-complete-date')
    const timeInput = document.body.querySelector<HTMLInputElement>('#appointment-complete-time')
    expect(dateInput?.value).toBe('2026-08-11')
    expect(timeInput?.value).toBe('16:45')

    dateInput!.value = '2026-08-10'
    dateInput!.dispatchEvent(new Event('input'))
    timeInput!.value = '09:15'
    timeInput!.dispatchEvent(new Event('input'))
    await flushPromises()

    const confirmButton = wrapper
      .findAllComponents(Button)
      .find((button) => button.text() === 'Bestätigen')
    await confirmButton!.trigger('click')
    await flushPromises()

    expect(appointmentService.completeAppointment).toHaveBeenCalledWith(
      '1',
      new Date(2026, 7, 10, 9, 15),
    )
    vi.useRealTimers()
  })

  it('shows a completed badge and a revert button for a completed appointment', async () => {
    vi.mocked(appointmentService.reopenAppointment).mockResolvedValue(createAppointment())
    const { wrapper } = await mountDrawer(
      createAppointment({ completed: true, actualEnd: new Date(2026, 7, 11, 14, 30) }),
    )

    expect(document.body.querySelector('.appointment-detail-drawer__badge--completed')).not.toBeNull()

    const reopenButton = wrapper
      .findAllComponents(Button)
      .find((button) => button.text() === 'Erledigt-Status zurücksetzen')
    await reopenButton!.trigger('click')
    await flushPromises()

    expect(appointmentService.reopenAppointment).toHaveBeenCalledWith('1')
  })

  it('asks for confirmation and deletes the appointment', async () => {
    const require = vi.fn<(options: { accept: () => void }) => void>((options) => options.accept())
    vi.mocked(useConfirm).mockReturnValue({ require } as never)
    vi.mocked(appointmentService.deleteAppointment).mockResolvedValue(undefined)
    const { wrapper } = await mountDrawer(createAppointment())

    const deleteButton = wrapper
      .findAllComponents(Button)
      .find((button) => button.text() === 'Termin löschen')
    await deleteButton!.trigger('click')
    await flushPromises()

    expect(appointmentService.deleteAppointment).toHaveBeenCalledWith('1', 'single')
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents?.[visibleEvents.length - 1]).toEqual([false])
  })
})
