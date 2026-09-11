import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import Dialog from 'primevue/dialog'
import { i18n } from '@/i18n'
import ComingSoonDialog from '@/components/layout/ComingSoonDialog.vue'

describe('ComingSoonDialog', () => {
  it('passes the localized header, message and visibility to the dialog', () => {
    const wrapper = shallowMount(ComingSoonDialog, {
      props: { visible: true },
      global: { plugins: [i18n], renderStubDefaultSlot: true },
    })
    const dialog = wrapper.findComponent(Dialog)

    // The auto-stub does not replicate Dialog's prop schema, so passed values surface as
    // plain HTML attributes on the stub element rather than as component props.
    expect(dialog.attributes('visible')).toBe('true')
    expect(dialog.attributes('header')).toBe('Bald verfügbar')
    expect(dialog.text()).toContain('Diese Seite befindet sich noch im Aufbau.')
  })
})
