import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Select from 'primevue/select'
import Button from 'primevue/button'
import ToggleSwitch from 'primevue/toggleswitch'
import { i18n } from '@/i18n'
import AppointmentFormDialog from '@/components/appointments/AppointmentFormDialog.vue'
import * as appointmentService from '@/services/appointmentService'
import * as propertyService from '@/services/propertyService'

vi.mock('@/services/appointmentService')
vi.mock('@/services/propertyService')

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(appointmentService.createAppointment).mockReset()
  vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
  vi.mocked(propertyService.fetchProperties).mockResolvedValue([
    { id: '1', name: 'Wohnanlage Sonnenhof', address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld', icon: 'pi-building' },
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

/** Finds an element inside the Dialog's teleported content by CSS selector. */
function bodyField(selector: string): DOMWrapper<Element> {
  return new DOMWrapper(document.body.querySelector(selector) as Element)
}

type DialogWrapper = Awaited<ReturnType<typeof mountDialog>>

/** Finds the submit button among every rendered PrimeVue Button by its label. */
function submitButton(wrapper: DialogWrapper) {
  return wrapper.findAllComponents(Button).find((button) => button.text() === 'Termin anlegen')!
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
        propertyName: 'Wohnanlage Sonnenhof',
        propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
        locked: false,
        recurring: false,
        recurrenceIntervalMonths: null,
      }),
    )
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents?.[visibleEvents.length - 1]).toEqual([false])
  })
})
