import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Select from 'primevue/select'
import Button from 'primevue/button'
import ToggleSwitch from 'primevue/toggleswitch'
import { i18n } from '@/i18n'
import AppointmentFormDialog from '@/components/appointments/AppointmentFormDialog.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { useAuthStore } from '@/stores/auth'
import * as appointmentService from '@/services/appointmentService'
import * as propertyService from '@/services/propertyService'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/appointmentService')
vi.mock('@/services/propertyService')

/** Builds a sample existing appointment for edit-mode tests, with overridable fields. */
function createExistingAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Kellerreinigung Q3',
    propertyId: '1',
    propertyName: 'Wohnanlage Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: 'Was ist zu tun?',
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

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(appointmentService.createAppointment).mockReset()
  vi.mocked(appointmentService.updateAppointment).mockReset()
  vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
  vi.mocked(propertyService.fetchProperties).mockResolvedValue([
    {
      id: '1',
      name: 'Wohnanlage Sonnenhof',
      address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
      icon: 'pi-building',
      latitude: 50.94,
      longitude: 6.88,
    },
  ])
  document.body.innerHTML = ''
})

/** Mounts the dialog already open; PrimeVue's Dialog teleports its content to document.body one microtask later. */
async function mountDialog() {
  const wrapper = mount(AppointmentFormDialog, {
    props: { visible: true },
    global: { plugins: [i18n, PrimeVue] },
    attachTo: document.body,
  })
  await flushPromises()
  return wrapper
}

/** Mounts the dialog already in edit mode for the given appointment: the component only pre-fills its fields on the closed-to-open transition, as it does in the app when `openEditDialog` is called while the dialog is closed. */
async function mountDialogForEdit(appointment: Appointment) {
  useAppointmentsStore().openEditDialog(appointment)
  const wrapper = mount(AppointmentFormDialog, {
    props: { visible: false },
    global: { plugins: [i18n, PrimeVue] },
    attachTo: document.body,
  })
  await wrapper.setProps({ visible: true })
  await flushPromises()
  return wrapper
}

/** Finds an element inside the Dialog's teleported content by CSS selector. */
function bodyField(selector: string): DOMWrapper<Element> {
  return new DOMWrapper(document.body.querySelector(selector) as Element)
}

type DialogWrapper = Awaited<ReturnType<typeof mountDialog>>

/** Finds the submit button among every rendered PrimeVue Button by its label. */
function submitButton(wrapper: DialogWrapper) {
  return wrapper.findAllComponents(Button).find((button) => button.text() === 'Termin anlegen')!
}

/** Finds the edit-mode save button among every rendered PrimeVue Button by its label. */
function saveButton(wrapper: DialogWrapper) {
  return wrapper.findAllComponents(Button).find((button) => button.text() === 'Speichern')!
}

/** Fills in the fields required for the form to be valid: title, property, day and time. */
async function fillRequiredFields(wrapper: DialogWrapper) {
  await bodyField('#appointment-title').setValue('Kellerreinigung Q3')
  const selects = wrapper.findAllComponents(Select)
  await selects[0]!.vm.$emit('update:modelValue', '1')
  await selects[1]!.vm.$emit('update:modelValue', '2026-08-11')
  await selects[2]!.vm.$emit('update:modelValue', '13:00')
}

describe('AppointmentFormDialog', () => {
  it('limits the title and description fields to their maximum length', async () => {
    await mountDialog()

    expect(bodyField('#appointment-title').attributes('maxlength')).toBe('50')
    expect(bodyField('#appointment-description').attributes('maxlength')).toBe('2000')
  })

  it('disables submit until title, property, day and time are filled in', async () => {
    const wrapper = await mountDialog()

    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await fillRequiredFields(wrapper)

    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('marks title, property, day, time and duration as required fields', async () => {
    await mountDialog()

    expect(bodyField('label[for="appointment-title"]').text()).toContain('*')
    expect(bodyField('.appointment-property-select__label').text()).toContain('*')
    expect(bodyField('label[for="appointment-day"]').text()).toContain('*')
    expect(bodyField('label[for="appointment-time"]').text()).toContain('*')
    expect(bodyField('label[for="appointment-duration"]').text()).toContain('*')
  })

  it('shows a hint under the title and property fields while they are empty, until they are filled in', async () => {
    const wrapper = await mountDialog()

    expect(bodyField('#appointment-title').classes()).toContain('p-invalid')
    expect(document.body.textContent).toContain('Bitte einen Titel eingeben.')
    expect(document.body.textContent).toContain('Bitte ein Objekt wählen.')

    await fillRequiredFields(wrapper)

    expect(bodyField('#appointment-title').classes()).not.toContain('p-invalid')
    expect(document.body.textContent).not.toContain('Bitte einen Titel eingeben.')
    expect(document.body.textContent).not.toContain('Bitte ein Objekt wählen.')
  })

  it('requires a recurrence interval once the recurring toggle is switched on', async () => {
    const wrapper = await mountDialog()
    await fillRequiredFields(wrapper)

    const toggles = wrapper.findAllComponents(ToggleSwitch)
    await toggles[1]!.vm.$emit('update:modelValue', true)

    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    const selects = wrapper.findAllComponents(Select)
    const recurrenceSelect = selects[selects.length - 1]!
    await recurrenceSelect.vm.$emit('update:modelValue', 3)

    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('creates the appointment with the resolved property and closes the dialog on submit', async () => {
    vi.mocked(appointmentService.createAppointment).mockResolvedValue({ id: '1' } as never)
    const wrapper = await mountDialog()
    await fillRequiredFields(wrapper)

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(appointmentService.createAppointment).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Kellerreinigung Q3',
        propertyId: '1',
        locked: false,
        recurring: false,
        recurrenceIntervalMonths: null,
      }),
    )
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents?.[visibleEvents.length - 1]).toEqual([false])
  })

  it('shows a demo account how many more appointments it may create', async () => {
    logInDemoAccount(2)

    await mountDialog()

    expect(document.body.textContent).toContain('Sie können noch 2 weitere Termine anlegen.')
  })

  it('keeps submit disabled once a demo account created all its additional appointments', async () => {
    logInDemoAccount(0)
    const wrapper = await mountDialog()
    await fillRequiredFields(wrapper)

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('bereits alle 3 zusätzlichen Termine angelegt')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    expect(appointmentService.createAppointment).not.toHaveBeenCalled()
  })

  it('pre-fills every field from the appointment being edited', async () => {
    const appointment = createExistingAppointment({
      title: 'Fensterreinigung',
      description: 'Vorhandene Beschreibung',
      locked: true,
      recurring: true,
      recurrenceIntervalMonths: 3,
      materials: ['Fensterwischer'],
    })

    const wrapper = await mountDialogForEdit(appointment)

    expect(bodyField('#appointment-title').element.getAttribute('value')).toBe('Fensterreinigung')
    expect((bodyField('#appointment-description').element as HTMLTextAreaElement).value).toBe(
      'Vorhandene Beschreibung',
    )

    const selects = wrapper.findAllComponents(Select)
    expect(selects[0]!.props('modelValue')).toBe('1')
    expect(selects[1]!.props('modelValue')).toBe('2026-08-11')
    expect(selects[2]!.props('modelValue')).toBe('13:00')
    expect(selects[3]!.props('modelValue')).toBe(120)
    expect(selects[4]!.props('modelValue')).toBe(3)

    const toggles = wrapper.findAllComponents(ToggleSwitch)
    expect(toggles[0]!.props('modelValue')).toBe(true)
    expect(toggles[1]!.props('modelValue')).toBe(true)

    expect(document.body.textContent).toContain('Fensterwischer')
    expect(bodyField('.p-dialog-title').text()).toBe('Termin bearbeiten')
    const buttonLabels = wrapper.findAllComponents(Button).map((button) => button.text())
    expect(buttonLabels).toContain('Speichern')
    expect(buttonLabels).not.toContain('Termin anlegen')
  })

  it("adds the edited appointment's own start day as a selectable option when it falls outside the generated upcoming-day list", async () => {
    const pastAppointment = createExistingAppointment({
      start: new Date(2020, 0, 5, 9, 0),
      end: new Date(2020, 0, 5, 10, 0),
    })

    const wrapper = await mountDialogForEdit(pastAppointment)

    const daySelect = wrapper.findAllComponents(Select)[1]!
    const dayOptionValues = (daySelect.props('options') as Array<{ value: string }>).map(
      (option) => option.value,
    )
    expect(dayOptionValues).toContain('2020-01-05')
    expect(daySelect.props('modelValue')).toBe('2020-01-05')
  })

  it('updates the appointment being edited instead of creating a new one', async () => {
    const existing = createExistingAppointment()
    vi.mocked(appointmentService.updateAppointment).mockResolvedValue(existing)
    const wrapper = await mountDialogForEdit(existing)

    await saveButton(wrapper).trigger('click')
    await flushPromises()

    expect(appointmentService.updateAppointment).toHaveBeenCalledWith(
      '1',
      expect.objectContaining({ title: 'Kellerreinigung Q3', propertyId: '1' }),
    )
    expect(appointmentService.createAppointment).not.toHaveBeenCalled()
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents?.[visibleEvents.length - 1]).toEqual([false])
  })

  it('shows an update-specific error and keeps the dialog open when updating fails', async () => {
    vi.mocked(appointmentService.updateAppointment).mockRejectedValue(new Error('network error'))
    const wrapper = await mountDialogForEdit(createExistingAppointment())

    await saveButton(wrapper).trigger('click')
    await flushPromises()

    expect(bodyField('.appointment-form-dialog__error').text()).toBe(
      'Der Termin konnte nicht aktualisiert werden.',
    )
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents).toBeUndefined()
  })

  it('still lets a demo account without remaining creations edit an existing appointment', async () => {
    logInDemoAccount(0)
    const wrapper = await mountDialogForEdit(createExistingAppointment())

    expect(saveButton(wrapper).attributes('disabled')).toBeUndefined()
    expect(document.body.textContent).not.toContain('zusätzlichen Termine')
  })
})

/** Logs in a demo account with the given number of remaining appointment creations. */
function logInDemoAccount(remainingAppointmentCreations: number) {
  useAuthStore().currentUser = {
    username: 'demo-1',
    displayName: 'Demo',
    demoAccount: true,
    expiresAt: new Date(),
    remainingPropertyCreations: 3,
    remainingAppointmentCreations,
  }
}
