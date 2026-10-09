import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { AxiosError, type AxiosResponse } from 'axios'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import { i18n } from '@/i18n'
import TenantFormDialog from '@/components/tenants/TenantFormDialog.vue'
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
  vi.mocked(apartmentService.fetchApartments).mockReset().mockResolvedValue([])
  vi.mocked(propertyService.fetchProperties).mockReset().mockResolvedValue([])
  vi.mocked(apartmentService.createApartment).mockReset().mockResolvedValue(createApartment())
  vi.mocked(apartmentService.addTenant).mockReset().mockResolvedValue(createApartment())
  vi.mocked(apartmentService.updateTenant).mockReset().mockResolvedValue(createApartment())
  document.body.innerHTML = ''
})

/** Mounts the dialog and opens it, as the app does when a form is requested while the dialog is closed: the component only pre-fills its fields on the closed-to-open transition. */
async function mountDialog(openForm: (store: ReturnType<typeof useTenantsStore>) => void) {
  openForm(useTenantsStore())
  const wrapper = mount(TenantFormDialog, {
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

/** Fills every field of the form of a new tenant with a new apartment. */
async function fillAllFields() {
  await enter('#tenant-first-name', 'Erika')
  await enter('#tenant-last-name', 'Mustermann')
  await enter('#tenant-floor', '2')
  await enter('#tenant-area', '64,5')
  await enter('#tenant-totalRent', '850')
  await enter('#tenant-coldRent', '650')
  await enter('#tenant-additionalCosts', '200')
}

describe('TenantFormDialog', () => {
  it('separates the tenant data from the apartment data', async () => {
    await mountDialog((store) => store.openCreateDialog('property-1'))

    const headings = Array.from(document.body.querySelectorAll('.tenant-form-dialog__section-heading')).map(
      (heading) => heading.textContent,
    )
    expect(headings).toEqual(['Daten des Mieters', 'Daten der Wohnung'])
    expect(document.body.textContent).toContain('Neuen Mieter anlegen')
  })

  it('limits both name fields to their maximum length', async () => {
    await mountDialog((store) => store.openCreateDialog('property-1'))

    expect(bodyField('#tenant-first-name').attributes('maxlength')).toBe('50')
    expect(bodyField('#tenant-last-name').attributes('maxlength')).toBe('50')
  })

  it('keeps the submit button disabled until every field is filled', async () => {
    const wrapper = await mountDialog((store) => store.openCreateDialog('property-1'))
    expect(buttonLabelled(wrapper, 'Mieter anlegen').attributes('disabled')).toBeDefined()

    await enter('#tenant-first-name', 'Erika')
    await enter('#tenant-last-name', 'Mustermann')
    await enter('#tenant-floor', '2')
    expect(buttonLabelled(wrapper, 'Mieter anlegen').attributes('disabled')).toBeDefined()

    await fillAllFields()
    expect(buttonLabelled(wrapper, 'Mieter anlegen').attributes('disabled')).toBeUndefined()
  })

  it('shows the required-field hints only for fields that were left empty after being touched', async () => {
    await mountDialog((store) => store.openCreateDialog('property-1'))
    expect(document.body.querySelector('.tenant-form-dialog__field-error')).toBeNull()

    await bodyField('#tenant-first-name').trigger('blur')
    await bodyField('#tenant-area').trigger('blur')
    await bodyField('#tenant-totalRent').trigger('blur')
    await flushPromises()

    const errors = Array.from(document.body.querySelectorAll('.tenant-form-dialog__field-error')).map(
      (error) => error.textContent?.trim(),
    )
    expect(errors).toEqual([
      'Bitte einen Vornamen eingeben.',
      'Bitte eine Wohnfläche größer als 0 eingeben.',
      'Bitte einen Betrag eingeben.',
    ])
  })

  it('creates the apartment with its first tenant and closes the dialog', async () => {
    const wrapper = await mountDialog((store) => store.openCreateDialog('property-1'))
    await enter('#tenant-first-name', '  Erika ')
    await enter('#tenant-last-name', 'Mustermann')
    await enter('#tenant-floor', '2')
    await enter('#tenant-area', '64,5')
    await enter('#tenant-totalRent', '850')
    await enter('#tenant-coldRent', '650')
    await enter('#tenant-additionalCosts', '200')

    await buttonLabelled(wrapper, 'Mieter anlegen').trigger('click')
    await flushPromises()

    expect(apartmentService.createApartment).toHaveBeenCalledWith('property-1', {
      apartment: { floor: 2, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200 },
      tenant: { firstName: 'Erika', lastName: 'Mustermann' },
    })
    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
  })

  it('accepts a basement floor and the ground floor', async () => {
    const wrapper = await mountDialog((store) => store.openCreateDialog('property-1'))
    await fillAllFields()

    await enter('#tenant-floor', '0')
    expect(buttonLabelled(wrapper, 'Mieter anlegen').attributes('disabled')).toBeUndefined()

    await enter('#tenant-floor', '-1')
    expect(buttonLabelled(wrapper, 'Mieter anlegen').attributes('disabled')).toBeUndefined()
  })

  it('shows the apartment data read-only when adding a further tenant and sends only the tenant', async () => {
    const wrapper = await mountDialog((store) => store.openAddTenantDialog(createApartment()))

    expect(document.body.textContent).toContain('Weiteren Mieter hinzufügen')
    expect(document.body.textContent).toContain('Die Wohnungsdaten gelten für alle Mieter dieser Wohnung.')
    expect(bodyField('#tenant-floor').attributes('disabled')).toBeDefined()
    expect((bodyField('#tenant-floor').element as HTMLInputElement).value).toBe('2')
    expect(bodyField('#tenant-first-name').attributes('disabled')).toBeUndefined()

    await enter('#tenant-first-name', 'Max')
    await enter('#tenant-last-name', 'Mustermann')
    await buttonLabelled(wrapper, 'Mieter hinzufügen').trigger('click')
    await flushPromises()

    expect(apartmentService.addTenant).toHaveBeenCalledWith('apartment-1', { firstName: 'Max', lastName: 'Mustermann' })
    expect(apartmentService.createApartment).not.toHaveBeenCalled()
  })

  it('pre-fills tenant and apartment when editing and saves both', async () => {
    const apartment = createApartment()
    const wrapper = await mountDialog((store) => store.openEditDialog(apartment, apartment.tenants[0]!))

    expect(document.body.textContent).toContain('Mieter bearbeiten')
    expect((bodyField('#tenant-first-name').element as HTMLInputElement).value).toBe('Erika')
    expect((bodyField('#tenant-last-name').element as HTMLInputElement).value).toBe('Mustermann')
    expect(bodyField('#tenant-floor').attributes('disabled')).toBeUndefined()
    expect(buttonLabelled(wrapper, 'Speichern').attributes('disabled')).toBeUndefined()

    await enter('#tenant-last-name', 'Musterfrau')
    await enter('#tenant-floor', '3')
    await buttonLabelled(wrapper, 'Speichern').trigger('click')
    await flushPromises()

    expect(apartmentService.updateTenant).toHaveBeenCalledWith('tenant-1', {
      apartment: { floor: 3, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200 },
      tenant: { firstName: 'Erika', lastName: 'Musterfrau' },
    })
    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
  })

  it('stays open and shows an error when saving fails', async () => {
    vi.mocked(apartmentService.createApartment).mockRejectedValue(new Error('500'))
    const wrapper = await mountDialog((store) => store.openCreateDialog('property-1'))
    await fillAllFields()

    await buttonLabelled(wrapper, 'Mieter anlegen').trigger('click')
    await flushPromises()

    expect(document.body.querySelector('.tenant-form-dialog__error')?.textContent).toBe(
      'Der Mieter konnte nicht gespeichert werden.',
    )
    expect(wrapper.emitted('update:visible')?.some(([isVisible]) => isVisible === false)).toBeFalsy()
  })

  it('shows the localized message the backend gave for a failed save', async () => {
    const limitError = new AxiosError('Request failed with status code 409')
    limitError.response = { status: 409, data: { message: 'Eine Wohnung kann höchstens 10 Mieter haben.' } } as AxiosResponse
    vi.mocked(apartmentService.createApartment).mockRejectedValue(limitError)
    const wrapper = await mountDialog((store) => store.openCreateDialog('property-1'))
    await fillAllFields()

    await buttonLabelled(wrapper, 'Mieter anlegen').trigger('click')
    await flushPromises()

    expect(document.body.querySelector('.tenant-form-dialog__error')?.textContent?.trim()).toBe(
      'Eine Wohnung kann höchstens 10 Mieter haben.',
    )
  })

  it('sends the form only once when the submit button is clicked twice in a row', async () => {
    let resolveCreate!: (apartment: Apartment) => void
    vi.mocked(apartmentService.createApartment).mockImplementation(
      () => new Promise((resolve) => (resolveCreate = resolve)),
    )
    const wrapper = await mountDialog((store) => store.openCreateDialog('property-1'))
    await fillAllFields()

    await buttonLabelled(wrapper, 'Mieter anlegen').trigger('click')
    await buttonLabelled(wrapper, 'Mieter anlegen').trigger('click')
    expect(buttonLabelled(wrapper, 'Mieter anlegen').attributes('disabled')).toBeDefined()
    resolveCreate(createApartment())
    await flushPromises()

    expect(apartmentService.createApartment).toHaveBeenCalledOnce()
  })

  it('shows no decimals for the floor', async () => {
    await mountDialog((store) => store.openCreateDialog('property-1'))

    await enter('#tenant-floor', '2,5')

    expect(Number.isInteger(Number((bodyField('#tenant-floor').element as HTMLInputElement).value.replace(',', '.')))).toBe(true)
  })

  it('tells a demo account how many more tenants it may create and blocks creating once they are used up', async () => {
    logInDemoAccount(1)
    const wrapper = await mountDialog((store) => store.openCreateDialog('property-1'))
    expect(document.body.querySelector('.demo-quota-hint')?.textContent).toContain('noch einen weiteren Mieter')
    await fillAllFields()
    expect(buttonLabelled(wrapper, 'Mieter anlegen').attributes('disabled')).toBeUndefined()

    logInDemoAccount(0)
    await flushPromises()

    expect(document.body.querySelector('.demo-quota-hint--exhausted')).not.toBeNull()
    expect(buttonLabelled(wrapper, 'Mieter anlegen').attributes('disabled')).toBeDefined()
    await buttonLabelled(wrapper, 'Mieter anlegen').trigger('click')
    expect(apartmentService.createApartment).not.toHaveBeenCalled()
  })

  it('still lets a demo account without tenant creations left edit a tenant', async () => {
    logInDemoAccount(0)
    const apartment = createApartment()
    const wrapper = await mountDialog((store) => store.openEditDialog(apartment, apartment.tenants[0]!))

    expect(document.body.querySelector('.demo-quota-hint')).toBeNull()
    expect(buttonLabelled(wrapper, 'Speichern').attributes('disabled')).toBeUndefined()
  })

  it('closes without saving when cancelled', async () => {
    const wrapper = await mountDialog((store) => store.openCreateDialog('property-1'))

    new DOMWrapper(document.body.querySelector('.tenant-form-dialog__cancel') as Element).trigger('click')
    await flushPromises()

    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
    expect(apartmentService.createApartment).not.toHaveBeenCalled()
  })

  it('starts blank again when reopened after an edit', async () => {
    const apartment = createApartment()
    const wrapper = await mountDialog((store) => store.openEditDialog(apartment, apartment.tenants[0]!))
    await wrapper.setProps({ visible: false })

    useTenantsStore().openCreateDialog('property-1')
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect((bodyField('#tenant-first-name').element as HTMLInputElement).value).toBe('')
    expect((bodyField('#tenant-floor').element as HTMLInputElement).value).toBe('')
  })
})
