import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import TenantNameFields from '@/components/tenants/TenantNameFields.vue'

/** Mounts the fields with the given props on top of two empty names. */
function mountFields(props: Record<string, unknown> = {}) {
  return mount(TenantNameFields, {
    props: { idPrefix: 'demo', firstName: '', lastName: '', ...props },
    global: { plugins: [i18n, PrimeVue] },
  })
}

describe('TenantNameFields', () => {
  it('derives the ids of both inputs from the given prefix and limits their length', () => {
    const wrapper = mountFields()

    expect(wrapper.find('#demo-first-name').attributes('maxlength')).toBe('50')
    expect(wrapper.find('#demo-last-name').attributes('maxlength')).toBe('50')
    expect(wrapper.find('label[for="demo-first-name"]').text()).toBe('Vorname *')
  })

  it('marks the fields as required by default', () => {
    const wrapper = mountFields()

    expect(wrapper.find('#demo-first-name').attributes('aria-required')).toBe('true')
    expect(wrapper.findAll('.required-field-label__marker')).toHaveLength(2)
  })

  it('shows neither asterisk nor required semantics when the fields are optional', () => {
    const wrapper = mountFields({ required: false })

    expect(wrapper.find('#demo-first-name').attributes('aria-required')).toBeUndefined()
    expect(wrapper.findAll('.required-field-label__marker')).toHaveLength(0)
  })

  it('shows the error of a name only when asked to and links it to its input', () => {
    const wrapper = mountFields({ showLastNameError: true })

    expect(wrapper.findAll('.tenant-name-fields__error').map((error) => error.text())).toEqual([
      'Bitte einen Nachnamen eingeben.',
    ])
    expect(wrapper.find('#demo-last-name').attributes('aria-describedby')).toBe('demo-last-name-error')
    expect(wrapper.find('#demo-first-name').attributes('aria-describedby')).toBeUndefined()
  })

  it('updates the names through v-model', async () => {
    const wrapper = mountFields()

    await wrapper.find('#demo-first-name').setValue('Erika')
    await wrapper.find('#demo-last-name').setValue('Mustermann')

    expect(wrapper.emitted('update:firstName')).toEqual([['Erika']])
    expect(wrapper.emitted('update:lastName')).toEqual([['Mustermann']])
  })

  it('emits which name was left', async () => {
    const wrapper = mountFields()

    await wrapper.find('#demo-first-name').trigger('blur')
    await wrapper.find('#demo-last-name').trigger('blur')

    expect(wrapper.emitted('blur-first-name')).toHaveLength(1)
    expect(wrapper.emitted('blur-last-name')).toHaveLength(1)
  })
})
