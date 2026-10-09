import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import { i18n } from '@/i18n'
import PropertyFormDialog from '@/components/properties/PropertyFormDialog.vue'
import PropertyLocationPreviewMap from '@/components/properties/PropertyLocationPreviewMap.vue'
import { usePropertiesStore } from '@/stores/properties'
import { useAuthStore } from '@/stores/auth'
import * as propertyService from '@/services/propertyService'
import * as geocodingService from '@/services/geocodingService'
import type { Property } from '@/types/property'

/** Builds a sample existing property for duplicate-check tests, with overridable fields. */
function createExistingProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: '1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln',
    icon: 'pi-building',
    latitude: null,
    longitude: null,
    ...overrides,
  }
}

vi.mock('@/services/propertyService')
vi.mock('@/services/geocodingService')

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(propertyService.createProperty).mockReset()
  vi.mocked(propertyService.updateProperty).mockReset()
  vi.mocked(geocodingService.geocodeAddress).mockReset()
  vi.mocked(geocodingService.validateAddress).mockReset()
  vi.mocked(geocodingService.validateAddress).mockResolvedValue({
    status: 'MATCH',
    latitude: null,
    longitude: null,
    suggestedStreet: null,
    suggestedHouseNumber: null,
    suggestedPostalCode: null,
    suggestedCity: null,
  })
  vi.useFakeTimers()
  document.body.innerHTML = ''
})

afterEach(() => {
  vi.useRealTimers()
})

/** Mounts the dialog already open; PrimeVue's Dialog teleports its content to document.body one microtask later. */
async function mountDialog() {
  const wrapper = mount(PropertyFormDialog, {
    props: { visible: true },
    global: {
      plugins: [i18n, PrimeVue],
      stubs: { PropertyLocationPreviewMap: true },
    },
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

/** Mounts the dialog already in edit mode for the given property: the component only pre-fills its fields on the closed-to-open transition, as it does in the app when `openEditDialog` is called while the dialog is closed. */
async function mountDialogForEdit(property: Property): Promise<DialogWrapper> {
  usePropertiesStore().openEditDialog(property)
  const wrapper = mount(PropertyFormDialog, {
    props: { visible: false },
    global: {
      plugins: [i18n, PrimeVue],
      stubs: { PropertyLocationPreviewMap: true },
    },
    attachTo: document.body,
  })
  await wrapper.setProps({ visible: true })
  await flushPromises()
  return wrapper
}

/** Finds the submit button among every rendered PrimeVue Button by its label. */
function submitButton(wrapper: DialogWrapper) {
  return wrapper.findAllComponents(Button).find((button) => button.text() === 'Objekt anlegen')!
}

/** Fills in every required field: name, street, house number, postal code and city. */
async function fillRequiredFields() {
  await bodyField('#property-name').setValue('Wohnanlage Nordpark')
  await bodyField('#property-street').setValue('Nordparkstr.')
  await bodyField('#property-house-number').setValue('3')
  await bodyField('#property-postal-code').setValue('50733')
  await bodyField('#property-city').setValue('Köln')
}

describe('PropertyFormDialog', () => {
  it('limits the name field to its maximum length', async () => {
    await mountDialog()

    expect(bodyField('#property-name').attributes('maxlength')).toBe('50')
  })

  it('disables submit until name, street, house number, postal code and city are filled in', async () => {
    const wrapper = await mountDialog()

    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await fillRequiredFields()

    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('marks name, street, house number, postal code and city as required fields, but not the address supplement', async () => {
    await mountDialog()

    expect(bodyField('label[for="property-name"]').text()).toContain('*')
    expect(bodyField('label[for="property-street"]').text()).toContain('*')
    expect(bodyField('label[for="property-house-number"]').text()).toContain('*')
    expect(bodyField('label[for="property-postal-code"]').text()).toContain('*')
    expect(bodyField('label[for="property-city"]').text()).toContain('*')
    expect(bodyField('label[for="property-address-supplement"]').text()).not.toContain('*')
  })

  it('shows no hint before the required fields are touched', async () => {
    await mountDialog()

    expect(bodyField('#property-name').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-street').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-house-number').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-postal-code').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-city').classes()).not.toContain('p-invalid')
  })

  it('shows a hint under every required field once it is touched and left empty, until it is filled in', async () => {
    await mountDialog()

    await bodyField('#property-name').trigger('blur')
    await bodyField('#property-street').trigger('blur')
    await bodyField('#property-house-number').trigger('blur')
    await bodyField('#property-postal-code').trigger('blur')
    await bodyField('#property-city').trigger('blur')

    expect(bodyField('#property-name').classes()).toContain('p-invalid')
    expect(bodyField('#property-street').classes()).toContain('p-invalid')
    expect(bodyField('#property-house-number').classes()).toContain('p-invalid')
    expect(bodyField('#property-postal-code').classes()).toContain('p-invalid')
    expect(bodyField('#property-city').classes()).toContain('p-invalid')
    expect(document.body.textContent).toContain('Bitte einen Namen eingeben.')
    expect(document.body.textContent).toContain('Bitte eine Straße eingeben.')
    expect(document.body.textContent).toContain('Bitte eine Hausnummer eingeben.')
    expect(document.body.textContent).toContain('Bitte eine Postleitzahl eingeben.')
    expect(document.body.textContent).toContain('Bitte einen Ort eingeben.')

    await fillRequiredFields()

    expect(bodyField('#property-name').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-street').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-house-number').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-postal-code').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-city').classes()).not.toContain('p-invalid')
  })

  it('keeps submit disabled while the house number is empty, even with an address supplement filled in', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-address-supplement').setValue('a')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('Köln')

    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('flags a postal code that is not exactly 5 digits with a hint and a red border, and disables submit', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('123')
    await bodyField('#property-city').setValue('Köln')

    expect(bodyField('#property-postal-code').classes()).toContain('p-invalid')
    expect(bodyField('.property-form-dialog__field-error').text()).toBe(
      'Die Postleitzahl muss aus genau 5 Ziffern bestehen.',
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('flags a street containing a symbol no real German street name contains, with a hint and a red border, and disables submit', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordpark@str.')
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('Köln')

    expect(bodyField('#property-street').classes()).toContain('p-invalid')
    expect(bodyField('.property-form-dialog__field-error').text()).toBe(
      "Die Straße darf nur Buchstaben, Ziffern, Leerzeichen sowie . - ' / enthalten.",
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('accepts a street with umlauts, a period, a hyphen and a digit', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue("Straße des 17. Juni - Königstraße'")
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('Köln')

    expect(bodyField('#property-street').classes()).not.toContain('p-invalid')
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('accepts a street with the typographic apostrophe, non-breaking space and en dash that "smart punctuation" autocorrects to', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('O’Connor–Weg West')
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('Köln')

    expect(bodyField('#property-street').classes()).not.toContain('p-invalid')
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('flags a house number that is not purely digits with a hint and a red border, and disables submit', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('3b')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('Köln')

    expect(bodyField('#property-house-number').classes()).toContain('p-invalid')
    expect(bodyField('.property-form-dialog__field-error').text()).toBe(
      'Die Hausnummer darf nur aus maximal 5 Ziffern bestehen.',
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('limits the house number field to 5 digits and rejects a longer numeric value', async () => {
    const wrapper = await mountDialog()

    expect(bodyField('#property-house-number').attributes('maxlength')).toBe('5')

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('123456')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('Köln')

    expect(bodyField('#property-house-number').classes()).toContain('p-invalid')
    expect(bodyField('.property-form-dialog__field-error').text()).toBe(
      'Die Hausnummer darf nur aus maximal 5 Ziffern bestehen.',
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('flags a city containing a symbol no real German place name contains, with a hint and a red border, and disables submit', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('Köln@Stadt')

    expect(bodyField('#property-city').classes()).toContain('p-invalid')
    expect(bodyField('.property-form-dialog__field-error').text()).toBe(
      "Der Ort darf nur Buchstaben, Ziffern, Leerzeichen sowie . - ' / enthalten.",
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('accepts a city with an umlaut, a period and a hyphen', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('St. Wendel-Bliesen')

    expect(bodyField('#property-city').classes()).not.toContain('p-invalid')
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('geocodes the combined address after a debounce and forwards the position to the preview map', async () => {
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue({ lat: 50.97, lng: 6.95 })
    const wrapper = await mountDialog()

    await fillRequiredFields()
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()

    expect(geocodingService.geocodeAddress).toHaveBeenCalledWith('Nordparkstr. 3, 50733 Köln')
    expect(wrapper.findComponent(PropertyLocationPreviewMap).props('position')).toEqual({
      lat: 50.97,
      lng: 6.95,
    })
  })

  it('creates the property without coordinates when the address cannot be geocoded', async () => {
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue(null)
    vi.mocked(propertyService.createProperty).mockResolvedValue({
      id: '3',
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      icon: 'pi-building',
      latitude: null,
      longitude: null,
    })
    const wrapper = await mountDialog()
    await fillRequiredFields()
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()

    expect(wrapper.findComponent(PropertyLocationPreviewMap).exists()).toBe(false)
    expect(bodyField('.property-form-dialog__map-empty').exists()).toBe(true)

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(propertyService.createProperty).toHaveBeenCalledWith({
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      latitude: null,
      longitude: null,
    })
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents?.[visibleEvents.length - 1]).toEqual([false])
  })

  it('prefers the coordinates confirmed by address validation over a failed preview geocode', async () => {
    vi.mocked(geocodingService.validateAddress).mockResolvedValue({
      status: 'MATCH',
      latitude: 50.9420135,
      longitude: 6.8771884,
      suggestedStreet: null,
      suggestedHouseNumber: null,
      suggestedPostalCode: null,
      suggestedCity: null,
    })
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue(null)
    vi.mocked(propertyService.createProperty).mockResolvedValue({
      id: '5',
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      icon: 'pi-building',
      latitude: 50.9420135,
      longitude: 6.8771884,
    })
    const wrapper = await mountDialog()
    await fillRequiredFields()
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(propertyService.createProperty).toHaveBeenCalledWith(
      expect.objectContaining({ latitude: 50.9420135, longitude: 6.8771884 }),
    )
  })

  it('creates the property with the combined address and geocoded coordinates, then closes the dialog', async () => {
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue({ lat: 50.97, lng: 6.95 })
    vi.mocked(propertyService.createProperty).mockResolvedValue({
      id: '2',
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      icon: 'pi-building',
      latitude: 50.97,
      longitude: 6.95,
    })
    const wrapper = await mountDialog()
    await fillRequiredFields()
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(propertyService.createProperty).toHaveBeenCalledWith({
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3, 50733 Köln',
      latitude: 50.97,
      longitude: 6.95,
    })
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents?.[visibleEvents.length - 1]).toEqual([false])
  })

  it('appends the address supplement directly to the house number, with no separator', async () => {
    vi.mocked(propertyService.createProperty).mockResolvedValue({
      id: '4',
      name: 'Wohnanlage Nordpark',
      address: 'Nordparkstr. 3a, 50733 Köln',
      icon: 'pi-building',
      latitude: null,
      longitude: null,
    })
    const wrapper = await mountDialog()
    await fillRequiredFields()
    await bodyField('#property-address-supplement').setValue('a')

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(propertyService.createProperty).toHaveBeenCalledWith(
      expect.objectContaining({ address: 'Nordparkstr. 3a, 50733 Köln' }),
    )
  })

  it('flags a duplicate name (case- and whitespace-insensitive) with a hint and disables submit, independently of the address', async () => {
    const store = usePropertiesStore()
    store.properties = [createExistingProperty({ name: '  wohnanlage nordpark  ' })]
    const wrapper = await mountDialog()

    await fillRequiredFields()

    expect(bodyField('#property-name').classes()).toContain('p-invalid')
    expect(bodyField('.property-form-dialog__field-error').text()).toBe(
      'Ein Objekt mit diesem Namen existiert bereits.',
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    // the address does not match the existing property, so only the name fields are flagged
    expect(bodyField('#property-street').classes()).not.toContain('p-invalid')
  })

  it('flags a duplicate address (case- and whitespace-insensitive) with a hint and disables submit, independently of the name', async () => {
    const store = usePropertiesStore()
    store.properties = [
      createExistingProperty({
        name: 'Ganz anderes Objekt',
        address: '  nordparkstr. 3, 50733 köln  ',
      }),
    ]
    const wrapper = await mountDialog()

    await fillRequiredFields()

    expect(bodyField('#property-street').classes()).toContain('p-invalid')
    expect(bodyField('#property-house-number').classes()).toContain('p-invalid')
    expect(bodyField('#property-postal-code').classes()).toContain('p-invalid')
    expect(bodyField('#property-city').classes()).toContain('p-invalid')
    const addressErrors = document.body.querySelectorAll('.property-form-dialog__field-error')
    expect(addressErrors[addressErrors.length - 1]?.textContent).toBe(
      'Ein Objekt mit dieser Adresse existiert bereits.',
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    // the name does not match the existing property, so the name field is not flagged
    expect(bodyField('#property-name').classes()).not.toContain('p-invalid')
  })

  it('shows an error and keeps the dialog open when creation fails', async () => {
    vi.mocked(propertyService.createProperty).mockRejectedValue(new Error('network error'))
    const wrapper = await mountDialog()
    await fillRequiredFields()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(bodyField('.property-form-dialog__error').exists()).toBe(true)
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents).toBeUndefined()
  })

  it('blocks submission and shows a correction suggestion for an address with a unique fix', async () => {
    vi.mocked(geocodingService.validateAddress).mockResolvedValue({
      status: 'SUGGESTION',
      latitude: null,
      longitude: null,
      suggestedStreet: 'Nordparkstr.',
      suggestedHouseNumber: '3',
      suggestedPostalCode: '50733',
      suggestedCity: 'Köln',
    })
    const wrapper = await mountDialog()
    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('99999')
    await bodyField('#property-city').setValue('Köln')

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(geocodingService.validateAddress).toHaveBeenCalledWith('Nordparkstr.', '3', '99999', 'Köln')
    expect(propertyService.createProperty).not.toHaveBeenCalled()
    expect(bodyField('.property-form-dialog__address-suggestion').text()).toContain(
      'Meinten Sie folgende Adresse: Nordparkstr. 3, 50733 Köln?',
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await bodyField('.property-form-dialog__accept-suggestion').trigger('click')

    expect(bodyField('#property-postal-code').element.getAttribute('value')).toBe('50733')
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('keeps a shown suggestion when only the address supplement changes, since that field is not part of the validated address', async () => {
    vi.mocked(geocodingService.validateAddress).mockResolvedValue({
      status: 'SUGGESTION',
      latitude: null,
      longitude: null,
      suggestedStreet: 'Nordparkstr.',
      suggestedHouseNumber: '3',
      suggestedPostalCode: '50733',
      suggestedCity: 'Köln',
    })
    const wrapper = await mountDialog()
    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('99999')
    await bodyField('#property-city').setValue('Köln')

    await submitButton(wrapper).trigger('click')
    await flushPromises()
    expect(bodyField('.property-form-dialog__address-suggestion').exists()).toBe(true)

    await bodyField('#property-address-supplement').setValue('a')

    expect(bodyField('.property-form-dialog__address-suggestion').exists()).toBe(true)
    expect(geocodingService.validateAddress).toHaveBeenCalledTimes(1)
  })

  it('blocks submission with a generic error when the address cannot be resolved at all', async () => {
    vi.mocked(geocodingService.validateAddress).mockResolvedValue({
      status: 'NOT_FOUND',
      latitude: null,
      longitude: null,
      suggestedStreet: null,
      suggestedHouseNumber: null,
      suggestedPostalCode: null,
      suggestedCity: null,
    })
    const wrapper = await mountDialog()
    await fillRequiredFields()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(propertyService.createProperty).not.toHaveBeenCalled()
    expect(bodyField('.property-form-dialog__field-error').text()).toBe(
      'Diese Adresse konnte nicht gefunden werden. Bitte prüfen Sie Ihre Eingabe.',
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('discards a stale validation response for an address the user has since changed, instead of creating it unchecked', async () => {
    let resolveFirstValidation!: (result: geocodingService.AddressValidationResult) => void
    vi.mocked(geocodingService.validateAddress).mockReturnValueOnce(
      new Promise((resolve) => {
        resolveFirstValidation = resolve
      }),
    )
    const wrapper = await mountDialog()
    await fillRequiredFields()

    await submitButton(wrapper).trigger('click')
    await flushPromises()
    // the user keeps editing while the first validation is still in flight
    await bodyField('#property-postal-code').setValue('99999')

    resolveFirstValidation({
      status: 'MATCH',
      latitude: null,
      longitude: null,
      suggestedStreet: null,
      suggestedHouseNumber: null,
      suggestedPostalCode: null,
      suggestedCity: null,
    })
    await flushPromises()

    expect(propertyService.createProperty).not.toHaveBeenCalled()
    // the edited (never-validated) address must not be silently approved by the stale response
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('does not create the property if the name became a duplicate while address validation was in flight', async () => {
    const store = usePropertiesStore()
    store.properties = [createExistingProperty({ name: 'Bereits Vergeben' })]
    let resolveValidation!: (result: geocodingService.AddressValidationResult) => void
    vi.mocked(geocodingService.validateAddress).mockReturnValueOnce(
      new Promise((resolve) => {
        resolveValidation = resolve
      }),
    )
    const wrapper = await mountDialog()
    await fillRequiredFields()

    await submitButton(wrapper).trigger('click')
    await flushPromises()
    // the user renames the property to a duplicate while the validation request is still in flight
    await bodyField('#property-name').setValue('Bereits Vergeben')

    resolveValidation({
      status: 'MATCH',
      latitude: null,
      longitude: null,
      suggestedStreet: null,
      suggestedHouseNumber: null,
      suggestedPostalCode: null,
      suggestedCity: null,
    })
    await flushPromises()

    expect(propertyService.createProperty).not.toHaveBeenCalled()
  })

  it('pre-fills every field from the property being edited', async () => {
    const wrapper = await mountDialogForEdit(
      createExistingProperty({ name: 'Wohnanlage Sonnenhof', address: 'Aachener Str. 512a, 50933 Köln' }),
    )

    expect(bodyField('#property-name').element.getAttribute('value')).toBe('Wohnanlage Sonnenhof')
    expect(bodyField('#property-street').element.getAttribute('value')).toBe('Aachener Str.')
    expect(bodyField('#property-house-number').element.getAttribute('value')).toBe('512')
    expect(bodyField('#property-address-supplement').element.getAttribute('value')).toBe('a')
    expect(bodyField('#property-postal-code').element.getAttribute('value')).toBe('50933')
    expect(bodyField('#property-city').element.getAttribute('value')).toBe('Köln')
    expect(bodyField('.p-dialog-title').text()).toBe('Objekt bearbeiten')
    const buttonLabels = wrapper.findAllComponents(Button).map((button) => button.text())
    expect(buttonLabels).toContain('Speichern')
    expect(buttonLabels).not.toContain('Objekt anlegen')
  })

  it('does not re-geocode the unchanged address when opening the edit dialog, keeping the property\'s own stored coordinates', async () => {
    const existing = createExistingProperty({ latitude: 50.94, longitude: 6.88 })
    await mountDialogForEdit(existing)
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()

    expect(geocodingService.geocodeAddress).not.toHaveBeenCalled()
    expect(bodyField('.property-form-dialog__map-empty').exists()).toBe(false)
  })

  it('resumes geocoding once the user actually changes the address being edited', async () => {
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue({ lat: 50.97, lng: 6.95 })
    const existing = createExistingProperty({ latitude: 50.94, longitude: 6.88 })
    await mountDialogForEdit(existing)

    await bodyField('#property-house-number').setValue('999')
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()

    expect(geocodingService.geocodeAddress).toHaveBeenCalledWith('Aachener Str. 999, 50933 Köln')
  })

  it('updates the property being edited instead of creating a new one, and does not flag its own name or address as a duplicate', async () => {
    const existing = createExistingProperty({
      name: 'Wohnanlage Sonnenhof',
      address: 'Aachener Str. 512, 50933 Köln',
    })
    usePropertiesStore().properties = [existing]
    vi.mocked(propertyService.updateProperty).mockResolvedValue({
      ...existing,
      address: 'Aachener Str. 512, 50933 Köln',
    })
    const wrapper = await mountDialogForEdit(existing)

    expect(bodyField('#property-name').classes()).not.toContain('p-invalid')
    expect(bodyField('#property-street').classes()).not.toContain('p-invalid')

    const saveButton = wrapper.findAllComponents(Button).find((button) => button.text() === 'Speichern')!
    await saveButton.trigger('click')
    await flushPromises()

    expect(propertyService.updateProperty).toHaveBeenCalledWith(
      '1',
      expect.objectContaining({ name: 'Wohnanlage Sonnenhof', address: 'Aachener Str. 512, 50933 Köln' }),
    )
    expect(propertyService.createProperty).not.toHaveBeenCalled()
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents?.[visibleEvents.length - 1]).toEqual([false])
  })

  it('shows an update-specific error and keeps the dialog open when updating fails', async () => {
    vi.mocked(propertyService.updateProperty).mockRejectedValue(new Error('network error'))
    const wrapper = await mountDialogForEdit(createExistingProperty())

    const saveButton = wrapper.findAllComponents(Button).find((button) => button.text() === 'Speichern')!
    await saveButton.trigger('click')
    await flushPromises()

    expect(bodyField('.property-form-dialog__error').text()).toBe(
      'Das Objekt konnte nicht aktualisiert werden.',
    )
    const visibleEvents = wrapper.emitted('update:visible')
    expect(visibleEvents).toBeUndefined()
  })

  it('resets every field when reopened', async () => {
    const wrapper = await mountDialog()
    await fillRequiredFields()

    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect(bodyField('#property-name').element.getAttribute('value')).toBeNull()
  })

  it('shows a demo account how many more properties it may create', async () => {
    logInDemoAccount(1)

    await mountDialog()

    expect(document.body.textContent).toContain('Sie können noch ein weiteres Objekt anlegen.')
  })

  it('blocks creating once a demo account created all its additional properties', async () => {
    logInDemoAccount(0)
    const wrapper = await mountDialog()
    await fillRequiredFields()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('bereits alle 3 zusätzlichen Objekte angelegt')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    expect(geocodingService.validateAddress).not.toHaveBeenCalled()
    expect(propertyService.createProperty).not.toHaveBeenCalled()
  })

  it('still lets a demo account without remaining creations edit an existing property', async () => {
    logInDemoAccount(0)
    const wrapper = await mountDialogForEdit(createExistingProperty())

    const saveButton = wrapper.findAllComponents(Button).find((button) => button.text() === 'Speichern')!

    expect(saveButton.attributes('disabled')).toBeUndefined()
    expect(document.body.textContent).not.toContain('zusätzlichen Objekte')
  })
})

/** Logs in a demo account with the given number of remaining property creations. */
function logInDemoAccount(remainingPropertyCreations: number) {
  useAuthStore().currentUser = {
    username: 'demo-1',
    displayName: 'Demo',
    demoAccount: true,
    expiresAt: new Date(),
    remainingPropertyCreations,
    remainingAppointmentCreations: 3,
    remainingTenantCreations: 10,
    appointmentBufferMinutes: 15,
  }
}
