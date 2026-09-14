import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ViewPlaceholder from '@/components/ViewPlaceholder.vue'

describe('ViewPlaceholder', () => {
  it('renders the given text', () => {
    const wrapper = mount(ViewPlaceholder, {
      props: { text: 'Hier ist die Übersichtsseite' },
    })

    expect(wrapper.text()).toBe('Hier ist die Übersichtsseite')
  })
})
