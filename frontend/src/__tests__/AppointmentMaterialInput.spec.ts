import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Chip from 'primevue/chip'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import AppointmentMaterialInput from '@/components/appointments/AppointmentMaterialInput.vue'

describe('AppointmentMaterialInput', () => {
  it('adds a material when Enter is pressed and clears the draft', async () => {
    const wrapper = mount(AppointmentMaterialInput, {
      props: { modelValue: [] },
      global: { plugins: [i18n, PrimeVue] },
    })

    const input = wrapper.find('input')
    await input.setValue('Kehrmaschine')
    await input.trigger('keydown.enter')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([['Kehrmaschine']])
  })

  it('ignores a blank or duplicate draft', async () => {
    const wrapper = mount(AppointmentMaterialInput, {
      props: { modelValue: ['Kehrmaschine'] },
      global: { plugins: [i18n, PrimeVue] },
    })

    const input = wrapper.find('input')
    await input.setValue('Kehrmaschine')
    await input.trigger('keydown.enter')
    await input.setValue('   ')
    await input.trigger('keydown.enter')

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('removes a material tag', () => {
    const wrapper = mount(AppointmentMaterialInput, {
      props: { modelValue: ['Kehrmaschine', 'Müllsäcke'] },
      global: { plugins: [i18n, PrimeVue] },
    })

    wrapper.findAllComponents(Chip)[0]!.vm.$emit('remove')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([['Müllsäcke']])
  })
})
