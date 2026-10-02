import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ProfileSection from '@/components/profile/ProfileSection.vue'

/** Mounts the section with the given feedback and slot content. */
function mountSection(props: { failureMessage?: string | null; successMessage?: string | null } = {}) {
  return mount(ProfileSection, {
    props: {
      headingId: 'sample-heading',
      heading: 'Beispiel',
      description: 'Eine Beschreibung.',
      failureMessage: null,
      successMessage: null,
      ...props,
    },
    slots: { default: '<input id="field" />', actions: '<button id="action">Speichern</button>' },
  })
}

describe('ProfileSection', () => {
  it('is labelled by its heading and shows heading and description', () => {
    const wrapper = mountSection()

    expect(wrapper.find('section').attributes('aria-labelledby')).toBe('sample-heading')
    expect(wrapper.find('#sample-heading').text()).toBe('Beispiel')
    expect(wrapper.text()).toContain('Eine Beschreibung.')
  })

  it('renders the fields before the feedback and the actions after it', () => {
    const wrapper = mountSection({ failureMessage: 'Fehler' })

    const html = wrapper.html()

    expect(html.indexOf('id="field"')).toBeLessThan(html.indexOf('Fehler'))
    expect(html.indexOf('Fehler')).toBeLessThan(html.indexOf('id="action"'))
  })

  it('announces a failure as alert and a success as status', () => {
    const wrapper = mountSection({ failureMessage: 'Fehler', successMessage: 'Gespeichert' })

    expect(wrapper.find('[role="alert"]').text()).toBe('Fehler')
    expect(wrapper.find('[role="status"]').text()).toBe('Gespeichert')
  })

  it('shows no feedback without messages', () => {
    const wrapper = mountSection()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.find('[role="status"]').exists()).toBe(false)
  })
})
