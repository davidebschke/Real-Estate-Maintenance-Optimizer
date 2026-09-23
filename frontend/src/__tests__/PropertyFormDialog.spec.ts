import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import { i18n } from '@/i18n'
import PropertyFormDialog from '@/components/properties/PropertyFormDialog.vue'
import PropertyLocationPreviewMap from '@/components/properties/PropertyLocationPreviewMap.vue'
import { usePropertiesStore } from '@/stores/properties'
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
  vi.mocked(geocodingService.geocodeAddress).mockReset()
  vi.mocked(geocodingService.validateAddress).mockReset()
  vi.mocked(geocodingService.validateAddress).mockResolvedValue({
    status: 'MATCH',
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

  it('flags a house number that is not purely digits with a hint and a red border, and disables submit', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('3b')
    await bodyField('#property-postal-code').setValue('50733')
    await bodyField('#property-city').setValue('Köln')

    expect(bodyField('#property-house-number').classes()).toContain('p-invalid')
    expect(bodyField('.property-form-dialog__field-error').text()).toBe(
      'Die Hausnummer darf nur aus Zahlen bestehen.',
    )
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
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

  it('blocks submission with a generic error when the address cannot be resolved at all', async () => {
    vi.mocked(geocodingService.validateAddress).mockResolvedValue({
      status: 'NOT_FOUND',
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

  it('resets every field when reopened', async () => {
    const wrapper = await mountDialog()
    await fillRequiredFields()

    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect(bodyField('#property-name').element.getAttribute('value')).toBeNull()
  })
})
