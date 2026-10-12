import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { AxiosError, type AxiosResponse } from 'axios'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import Select from 'primevue/select'
import { i18n } from '@/i18n'
import ApartmentFormDialog from '@/components/tenants/ApartmentFormDialog.vue'
import { useTenantsStore } from '@/stores/tenants'
import { useAuthStore } from '@/stores/auth'
import * as apartmentService from '@/services/apartmentService'
import * as propertyService from '@/services/propertyService'
import type { Apartment } from '@/types/apartment'

vi.mock('@/services/apartmentService')
vi.mock('@/services/propertyService')

/** Builds a sample apartment with one tenant, with overridable fields. */
function createApartment(overrides: Partial<Apartment> = {}): Apartment {
  return {
    id: 'apartment-1',
    propertyId: 'property-1',
    floor: 2,
    areaSquareMeters: 64.5,
    totalRent: 850,
    coldRent: 650,
    additionalCosts: 200,
    tenants: [{ id: 'tenant-1', firstName: 'Erika', lastName: 'Mustermann' }],
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 12))
  vi.mocked(apartmentService.fetchApartments).mockReset().mockResolvedValue([])
  vi.mocked(propertyService.fetchProperties).mockReset().mockResolvedValue([])
  vi.mocked(apartmentService.createApartment).mockReset().mockResolvedValue(createApartment())
  vi.mocked(apartmentService.updateApartment).mockReset().mockResolvedValue(createApartment())
  document.body.innerHTML = ''
})

afterEach(() => {
  vi.useRealTimers()
})

/** Mounts the dialog and opens it, as the app does when a form is requested while the dialog is closed: the component only pre-fills its fields on the closed-to-open transition. */
async function mountDialog(openForm: (store: ReturnType<typeof useTenantsStore>) => void) {
  openForm(useTenantsStore())
  const wrapper = mount(ApartmentFormDialog, {
    props: { visible: false },
    global: { plugins: [i18n, PrimeVue] },
    attachTo: document.body,
  })
  await wrapper.setProps({ visible: true })
  await flushPromises()
  return wrapper
}

type DialogWrapper = Awaited<ReturnType<typeof mountDialog>>

/** Logs in a demo account with the given number of remaining tenant creations directly in the store. */
function logInDemoAccount(remainingTenantCreations: number) {
  useAuthStore().currentUser = {
    username: 'demo-1',
    displayName: 'Demo',
    demoAccount: true,
    expiresAt: new Date(),
    remainingPropertyCreations: 3,
    remainingAppointmentCreations: 3,
    remainingTenantCreations,
    remainingAiOptimizations: 1,
    appointmentBufferMinutes: 15,
  }
}

/** Finds an element inside the Dialog's teleported content by CSS selector. */
function bodyField(selector: string): DOMWrapper<Element> {
  return new DOMWrapper(document.body.querySelector(selector) as Element)
}

/** Types a value into a field and leaves it, which is when a PrimeVue number input commits its value. */
async function enter(selector: string, value: string) {
  const field = bodyField(selector)
  await field.setValue(value)
  await field.trigger('blur')
}

/** Finds a PrimeVue Button of the dialog by its label. */
function buttonLabelled(wrapper: DialogWrapper, label: string) {
  return wrapper.findAllComponents(Button).find((button) => button.text() === label)!
}

/** Fills every apartment field of the form. */
async function fillApartmentFields() {
  await enter('#apartment-floor', '2')
  await enter('#apartment-area', '64,5')
  await enter('#apartment-totalRent', '850')
  await enter('#apartment-coldRent', '650')
  await enter('#apartment-additionalCosts', '200')
}

describe('ApartmentFormDialog', () => {
  describe('creating an apartment', () => {
    it('offers the apartment data and an optional first tenant as separate sections', async () => {
      await mountDialog((store) => store.openCreateApartmentDialog('property-1'))

      const headings = Array.from(document.body.querySelectorAll('.apartment-form-dialog__section-heading')).map(
        (heading) => heading.textContent,
      )
      expect(headings).toEqual(['Daten der Wohnung', 'Ersten Mieter anlegen (optional)'])
      expect(document.body.textContent).toContain('Wohnung hinzufügen')
      expect(document.body.querySelector('#apartment-rent-effective-month')).toBeNull()
    })

    it('keeps the submit button disabled until every apartment field is filled', async () => {
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeDefined()

      await enter('#apartment-floor', '2')
      await enter('#apartment-area', '64,5')
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeDefined()

      await fillApartmentFields()
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeUndefined()
    })

    it('shows the required-field hints only for fields that were left empty after being touched', async () => {
      await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      expect(document.body.querySelector('.apartment-form-dialog__field-error')).toBeNull()

      await bodyField('#apartment-area').trigger('blur')
      await bodyField('#apartment-totalRent').trigger('blur')
      await flushPromises()

      const errors = Array.from(document.body.querySelectorAll('.apartment-form-dialog__field-error')).map(
        (error) => error.textContent?.trim(),
      )
      expect(errors).toEqual(['Bitte eine Wohnfläche größer als 0 eingeben.', 'Bitte einen Betrag eingeben.'])
    })

    it('accepts a basement floor and the ground floor', async () => {
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      await fillApartmentFields()

      await enter('#apartment-floor', '0')
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeUndefined()

      await enter('#apartment-floor', '-1')
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeUndefined()
    })

    it('shows no decimals for the floor', async () => {
      await mountDialog((store) => store.openCreateApartmentDialog('property-1'))

      await enter('#apartment-floor', '2,5')

      expect(Number.isInteger(Number((bodyField('#apartment-floor').element as HTMLInputElement).value.replace(',', '.')))).toBe(true)
    })

    it('creates a vacant apartment when the first tenant section stays empty and closes the dialog', async () => {
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      await fillApartmentFields()

      await buttonLabelled(wrapper, 'Wohnung anlegen').trigger('click')
      await flushPromises()

      expect(apartmentService.createApartment).toHaveBeenCalledWith('property-1', {
        apartment: { floor: 2, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200 },
      })
      expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
    })

    it('creates the apartment together with its first tenant when both names are filled', async () => {
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      await fillApartmentFields()
      await enter('#apartment-tenant-first-name', '  Erika ')
      await enter('#apartment-tenant-last-name', 'Mustermann')

      await buttonLabelled(wrapper, 'Wohnung anlegen').trigger('click')
      await flushPromises()

      expect(apartmentService.createApartment).toHaveBeenCalledWith('property-1', {
        apartment: { floor: 2, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200 },
        tenant: { firstName: 'Erika', lastName: 'Mustermann' },
      })
    })

    it('refuses a first tenant of which only one name is filled and names the missing one', async () => {
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      await fillApartmentFields()

      await enter('#apartment-tenant-first-name', 'Erika')

      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeDefined()
      const errors = Array.from(document.body.querySelectorAll('.tenant-name-fields__error')).map(
        (error) => error.textContent?.trim(),
      )
      expect(errors).toEqual(['Bitte einen Nachnamen eingeben.'])

      await enter('#apartment-tenant-last-name', 'Mustermann')
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeUndefined()
    })

    it('does not mark the optional name fields as required', async () => {
      await mountDialog((store) => store.openCreateApartmentDialog('property-1'))

      expect(bodyField('#apartment-tenant-first-name').attributes('aria-required')).toBeUndefined()
      expect(document.body.querySelectorAll('.tenant-name-fields .required-field-label__marker')).toHaveLength(0)
    })

    it('stays open and shows an error when saving fails', async () => {
      vi.mocked(apartmentService.createApartment).mockRejectedValue(new Error('500'))
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      await fillApartmentFields()

      await buttonLabelled(wrapper, 'Wohnung anlegen').trigger('click')
      await flushPromises()

      expect(document.body.querySelector('.apartment-form-dialog__error')?.textContent).toBe(
        'Die Wohnung konnte nicht gespeichert werden.',
      )
      expect(wrapper.emitted('update:visible')?.some(([isVisible]) => isVisible === false)).toBeFalsy()
    })

    it('shows the localized message the backend gave for a failed save', async () => {
      const limitError = new AxiosError('Request failed with status code 409')
      limitError.response = { status: 409, data: { message: 'Das Limit ist erreicht.' } } as AxiosResponse
      vi.mocked(apartmentService.createApartment).mockRejectedValue(limitError)
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      await fillApartmentFields()

      await buttonLabelled(wrapper, 'Wohnung anlegen').trigger('click')
      await flushPromises()

      expect(document.body.querySelector('.apartment-form-dialog__error')?.textContent?.trim()).toBe('Das Limit ist erreicht.')
    })

    it('sends the form only once when the submit button is clicked twice in a row', async () => {
      let resolveCreate!: (apartment: Apartment) => void
      vi.mocked(apartmentService.createApartment).mockImplementation(
        () => new Promise((resolve) => (resolveCreate = resolve)),
      )
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      await fillApartmentFields()

      await buttonLabelled(wrapper, 'Wohnung anlegen').trigger('click')
      await buttonLabelled(wrapper, 'Wohnung anlegen').trigger('click')
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeDefined()
      resolveCreate(createApartment())
      await flushPromises()

      expect(apartmentService.createApartment).toHaveBeenCalledOnce()
    })

    it('tells a demo account how many more tenants it may create', async () => {
      logInDemoAccount(2)
      await mountDialog((store) => store.openCreateApartmentDialog('property-1'))

      expect(document.body.querySelector('.demo-quota-hint')?.textContent).toContain('noch 2 weitere Mieter')
    })

    it('blocks only the creation with a first tenant once the demo tenant creations are used up', async () => {
      logInDemoAccount(0)
      const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))
      await fillApartmentFields()
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeUndefined()

      await enter('#apartment-tenant-first-name', 'Erika')
      await enter('#apartment-tenant-last-name', 'Mustermann')
      expect(buttonLabelled(wrapper, 'Wohnung anlegen').attributes('disabled')).toBeDefined()
      await buttonLabelled(wrapper, 'Wohnung anlegen').trigger('click')
      expect(apartmentService.createApartment).not.toHaveBeenCalled()
    })
  })

  describe('editing an apartment', () => {
    it('pre-fills the stored apartment data and offers no tenant section', async () => {
      const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))

      expect(document.body.textContent).toContain('Wohnung bearbeiten')
      expect((bodyField('#apartment-floor').element as HTMLInputElement).value).toBe('2')
      expect((bodyField('#apartment-area').element as HTMLInputElement).value).toContain('64,5')
      expect(document.body.querySelector('.tenant-name-fields')).toBeNull()
      expect(document.body.querySelector('.demo-quota-hint')).toBeNull()
      expect(buttonLabelled(wrapper, 'Speichern').attributes('disabled')).toBeUndefined()
    })

    it('saves a change that leaves the rent alone without a month', async () => {
      const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))

      await enter('#apartment-floor', '3')
      expect(document.body.querySelector('#apartment-rent-effective-month')).toBeNull()
      await buttonLabelled(wrapper, 'Speichern').trigger('click')
      await flushPromises()

      expect(apartmentService.updateApartment).toHaveBeenCalledWith('apartment-1', {
        apartment: { floor: 3, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200 },
        rentEffectiveMonth: null,
      })
      expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
    })

    it('asks from which month a changed total rent applies, defaulting to the current month', async () => {
      const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))

      await enter('#apartment-totalRent', '900')

      const select = wrapper.findComponent(Select)
      expect(select.exists()).toBe(true)
      expect(select.props('modelValue')).toBe('2026-10')
      expect(document.body.querySelector('label[for="apartment-rent-effective-month"]')?.textContent).toContain('Gilt ab')
      expect(document.body.textContent).toContain('frühere Monate behalten den bisherigen Mietpreis')
    })

    it('offers the months from the current month back to January 2000, newest first', async () => {
      const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))
      await enter('#apartment-totalRent', '900')

      const options = wrapper.findComponent(Select).props('options') as Array<{ value: string; label: string }>

      expect(options[0]).toEqual({ value: '2026-10', label: 'Oktober 2026' })
      expect(options[options.length - 1]).toEqual({ value: '2000-01', label: 'Januar 2000' })
      expect(options).toHaveLength(26 * 12 + 10)
    })

    it('sends the chosen month as rentEffectiveMonth together with the new rent', async () => {
      const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))
      await enter('#apartment-totalRent', '900')
      await wrapper.findComponent(Select).vm.$emit('update:modelValue', '2026-03')
      await flushPromises()

      await buttonLabelled(wrapper, 'Speichern').trigger('click')
      await flushPromises()

      expect(apartmentService.updateApartment).toHaveBeenCalledWith('apartment-1', {
        apartment: { floor: 2, areaSquareMeters: 64.5, totalRent: 900, coldRent: 650, additionalCosts: 200 },
        rentEffectiveMonth: '2026-03',
      })
    })

    it('drops the month again when the total rent is changed back to the stored value', async () => {
      const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))
      await enter('#apartment-totalRent', '900')
      await wrapper.findComponent(Select).vm.$emit('update:modelValue', '2026-03')
      await enter('#apartment-totalRent', '850')
      await flushPromises()

      expect(document.body.querySelector('#apartment-rent-effective-month')).toBeNull()
      await buttonLabelled(wrapper, 'Speichern').trigger('click')
      await flushPromises()

      expect(vi.mocked(apartmentService.updateApartment).mock.calls[0]![1].rentEffectiveMonth).toBeNull()
    })

    it('still lets a demo account without tenant creations left edit an apartment', async () => {
      logInDemoAccount(0)
      const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))

      expect(buttonLabelled(wrapper, 'Speichern').attributes('disabled')).toBeUndefined()
    })

    it('starts the month choice at the current month again when reopened', async () => {
      const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))
      await enter('#apartment-totalRent', '900')
      await wrapper.findComponent(Select).vm.$emit('update:modelValue', '2026-03')
      await wrapper.setProps({ visible: false })

      useTenantsStore().openEditApartmentDialog(createApartment())
      await wrapper.setProps({ visible: true })
      await flushPromises()
      await enter('#apartment-totalRent', '900')

      expect(wrapper.findComponent(Select).props('modelValue')).toBe('2026-10')
    })
  })

  it('closes without saving when cancelled', async () => {
    const wrapper = await mountDialog((store) => store.openCreateApartmentDialog('property-1'))

    new DOMWrapper(document.body.querySelector('.apartment-form-dialog__cancel') as Element).trigger('click')
    await flushPromises()

    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
    expect(apartmentService.createApartment).not.toHaveBeenCalled()
  })

  it('starts blank again when reopened after an edit', async () => {
    const wrapper = await mountDialog((store) => store.openEditApartmentDialog(createApartment()))
    await wrapper.setProps({ visible: false })

    useTenantsStore().openCreateApartmentDialog('property-1')
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect((bodyField('#apartment-floor').element as HTMLInputElement).value).toBe('')
    expect((bodyField('#apartment-tenant-first-name').element as HTMLInputElement).value).toBe('')
  })
})
