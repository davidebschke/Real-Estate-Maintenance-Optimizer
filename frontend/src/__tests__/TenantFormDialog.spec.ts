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

/** Types a value into a field and leaves it. */
async function enter(selector: string, value: string) {
  const field = bodyField(selector)
  await field.setValue(value)
  await field.trigger('blur')
}

/** Finds a PrimeVue Button of the dialog by its label. */
function buttonLabelled(wrapper: DialogWrapper, label: string) {
  return wrapper.findAllComponents(Button).find((button) => button.text() === label)!
}

/** Fills both name fields of the form. */
async function fillNames() {
  await enter('#tenant-first-name', 'Max')
  await enter('#tenant-last-name', 'Mustermann')
}

describe('TenantFormDialog', () => {
  it('shows only the name fields, without any apartment data', async () => {
    await mountDialog((store) => store.openAddTenantDialog(createApartment()))

    expect(document.body.textContent).toContain('Mieter hinzufügen')
    expect(document.body.querySelector('#tenant-first-name')).not.toBeNull()
    expect(document.body.querySelector('#tenant-last-name')).not.toBeNull()
    expect(document.body.querySelector('#tenant-floor')).toBeNull()
    expect(document.body.querySelector('.tenant-form-dialog__section-heading')).toBeNull()
  })

  it('limits both name fields to their maximum length', async () => {
    await mountDialog((store) => store.openAddTenantDialog(createApartment()))

    expect(bodyField('#tenant-first-name').attributes('maxlength')).toBe('50')
    expect(bodyField('#tenant-last-name').attributes('maxlength')).toBe('50')
  })

  it('keeps the submit button disabled until both names are filled', async () => {
    const wrapper = await mountDialog((store) => store.openAddTenantDialog(createApartment()))
    expect(buttonLabelled(wrapper, 'Mieter hinzufügen').attributes('disabled')).toBeDefined()

    await enter('#tenant-first-name', 'Max')
    expect(buttonLabelled(wrapper, 'Mieter hinzufügen').attributes('disabled')).toBeDefined()

    await enter('#tenant-last-name', 'Mustermann')
    expect(buttonLabelled(wrapper, 'Mieter hinzufügen').attributes('disabled')).toBeUndefined()
  })

  it('shows the required-field hints only for names that were left empty after being touched', async () => {
    await mountDialog((store) => store.openAddTenantDialog(createApartment()))
    expect(document.body.querySelector('.tenant-name-fields__error')).toBeNull()

    await bodyField('#tenant-first-name').trigger('blur')
    await flushPromises()

    const errors = Array.from(document.body.querySelectorAll('.tenant-name-fields__error')).map(
      (error) => error.textContent?.trim(),
    )
    expect(errors).toEqual(['Bitte einen Vornamen eingeben.'])
  })

  it('adds a tenant to the apartment and closes the dialog', async () => {
    const wrapper = await mountDialog((store) => store.openAddTenantDialog(createApartment()))
    await enter('#tenant-first-name', '  Max ')
    await enter('#tenant-last-name', 'Mustermann')

    await buttonLabelled(wrapper, 'Mieter hinzufügen').trigger('click')
    await flushPromises()

    expect(apartmentService.addTenant).toHaveBeenCalledWith('apartment-1', { firstName: 'Max', lastName: 'Mustermann' })
    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
  })

  it('pre-fills the name when renaming and saves only the names', async () => {
    const apartment = createApartment()
    const wrapper = await mountDialog((store) => store.openEditTenantDialog(apartment, apartment.tenants[0]!))

    expect(document.body.textContent).toContain('Mieter bearbeiten')
    expect((bodyField('#tenant-first-name').element as HTMLInputElement).value).toBe('Erika')
    expect((bodyField('#tenant-last-name').element as HTMLInputElement).value).toBe('Mustermann')
    expect(buttonLabelled(wrapper, 'Speichern').attributes('disabled')).toBeUndefined()

    await enter('#tenant-last-name', 'Musterfrau')
    await buttonLabelled(wrapper, 'Speichern').trigger('click')
    await flushPromises()

    expect(apartmentService.updateTenant).toHaveBeenCalledWith('tenant-1', { firstName: 'Erika', lastName: 'Musterfrau' })
    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
  })

  it('stays open and shows an error when saving fails', async () => {
    vi.mocked(apartmentService.addTenant).mockRejectedValue(new Error('500'))
    const wrapper = await mountDialog((store) => store.openAddTenantDialog(createApartment()))
    await fillNames()

    await buttonLabelled(wrapper, 'Mieter hinzufügen').trigger('click')
    await flushPromises()

    expect(document.body.querySelector('.tenant-form-dialog__error')?.textContent).toBe(
      'Der Mieter konnte nicht gespeichert werden.',
    )
    expect(wrapper.emitted('update:visible')?.some(([isVisible]) => isVisible === false)).toBeFalsy()
  })

  it('shows the localized message the backend gave for a failed save', async () => {
    const limitError = new AxiosError('Request failed with status code 409')
    limitError.response = { status: 409, data: { message: 'Eine Wohnung kann höchstens 10 Mieter haben.' } } as AxiosResponse
    vi.mocked(apartmentService.addTenant).mockRejectedValue(limitError)
    const wrapper = await mountDialog((store) => store.openAddTenantDialog(createApartment()))
    await fillNames()

    await buttonLabelled(wrapper, 'Mieter hinzufügen').trigger('click')
    await flushPromises()

    expect(document.body.querySelector('.tenant-form-dialog__error')?.textContent?.trim()).toBe(
      'Eine Wohnung kann höchstens 10 Mieter haben.',
    )
  })

  it('sends the form only once when the submit button is clicked twice in a row', async () => {
    let resolveAdd!: (apartment: Apartment) => void
    vi.mocked(apartmentService.addTenant).mockImplementation(() => new Promise((resolve) => (resolveAdd = resolve)))
    const wrapper = await mountDialog((store) => store.openAddTenantDialog(createApartment()))
    await fillNames()

    await buttonLabelled(wrapper, 'Mieter hinzufügen').trigger('click')
    await buttonLabelled(wrapper, 'Mieter hinzufügen').trigger('click')
    expect(buttonLabelled(wrapper, 'Mieter hinzufügen').attributes('disabled')).toBeDefined()
    resolveAdd(createApartment())
    await flushPromises()

    expect(apartmentService.addTenant).toHaveBeenCalledOnce()
  })

  it('tells a demo account how many more tenants it may create and blocks adding once they are used up', async () => {
    logInDemoAccount(1)
    const wrapper = await mountDialog((store) => store.openAddTenantDialog(createApartment()))
    expect(document.body.querySelector('.demo-quota-hint')?.textContent).toContain('noch einen weiteren Mieter')
    await fillNames()
    expect(buttonLabelled(wrapper, 'Mieter hinzufügen').attributes('disabled')).toBeUndefined()

    logInDemoAccount(0)
    await flushPromises()

    expect(document.body.querySelector('.demo-quota-hint--exhausted')).not.toBeNull()
    expect(buttonLabelled(wrapper, 'Mieter hinzufügen').attributes('disabled')).toBeDefined()
    await buttonLabelled(wrapper, 'Mieter hinzufügen').trigger('click')
    expect(apartmentService.addTenant).not.toHaveBeenCalled()
  })

  it('still lets a demo account without tenant creations left rename a tenant', async () => {
    logInDemoAccount(0)
    const apartment = createApartment()
    const wrapper = await mountDialog((store) => store.openEditTenantDialog(apartment, apartment.tenants[0]!))

    expect(document.body.querySelector('.demo-quota-hint')).toBeNull()
    expect(buttonLabelled(wrapper, 'Speichern').attributes('disabled')).toBeUndefined()
  })

  it('closes without saving when cancelled', async () => {
    const wrapper = await mountDialog((store) => store.openAddTenantDialog(createApartment()))

    new DOMWrapper(document.body.querySelector('.tenant-form-dialog__cancel') as Element).trigger('click')
    await flushPromises()

    expect(wrapper.emitted('update:visible')?.slice(-1)).toEqual([[false]])
    expect(apartmentService.addTenant).not.toHaveBeenCalled()
  })

  it('starts blank again when reopened after a rename', async () => {
    const apartment = createApartment()
    const wrapper = await mountDialog((store) => store.openEditTenantDialog(apartment, apartment.tenants[0]!))
    await wrapper.setProps({ visible: false })

    useTenantsStore().openAddTenantDialog(apartment)
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect((bodyField('#tenant-first-name').element as HTMLInputElement).value).toBe('')
    expect((bodyField('#tenant-last-name').element as HTMLInputElement).value).toBe('')
  })
})
