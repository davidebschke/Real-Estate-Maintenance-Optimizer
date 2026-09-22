import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import { i18n } from '@/i18n'
import PropertyFormDialog from '@/components/properties/PropertyFormDialog.vue'
import PropertyLocationPreviewMap from '@/components/properties/PropertyLocationPreviewMap.vue'
import * as propertyService from '@/services/propertyService'
import * as geocodingService from '@/services/geocodingService'

vi.mock('@/services/propertyService')
vi.mock('@/services/geocodingService')

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(propertyService.createProperty).mockReset()
  vi.mocked(geocodingService.geocodeAddress).mockReset()
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

  it('keeps submit disabled for a postal code that is not exactly 5 digits', async () => {
    const wrapper = await mountDialog()

    await bodyField('#property-name').setValue('Wohnanlage Nordpark')
    await bodyField('#property-street').setValue('Nordparkstr.')
    await bodyField('#property-house-number').setValue('3')
    await bodyField('#property-postal-code').setValue('123')
    await bodyField('#property-city').setValue('Köln')

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

  it('resets every field when reopened', async () => {
    const wrapper = await mountDialog()
    await fillRequiredFields()

    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect(bodyField('#property-name').element.getAttribute('value')).toBeNull()
  })
})
