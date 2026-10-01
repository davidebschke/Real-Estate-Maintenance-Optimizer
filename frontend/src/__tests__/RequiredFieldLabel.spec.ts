import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'

describe('RequiredFieldLabel', () => {
  it('renders a label bound to the given field id', () => {
    const wrapper = mount(RequiredFieldLabel, { props: { fieldId: 'field-1', label: 'Titel' } })

    expect(wrapper.element.tagName).toBe('LABEL')
    expect(wrapper.attributes('for')).toBe('field-1')
    expect(wrapper.text()).toContain('Titel')
  })

  it('omits the required marker when the field is not required', () => {
    const wrapper = mount(RequiredFieldLabel, {
      props: { fieldId: 'field-1', label: 'Titel', required: false },
    })

    expect(wrapper.find('.required-field-label__marker').exists()).toBe(false)
  })

  it('shows a required marker hidden from assistive technology', () => {
    const wrapper = mount(RequiredFieldLabel, { props: { fieldId: 'field-1', label: 'Titel' } })

    const marker = wrapper.find('.required-field-label__marker')
    expect(marker.text()).toBe('*')
    expect(marker.attributes('aria-hidden')).toBe('true')
  })
})
