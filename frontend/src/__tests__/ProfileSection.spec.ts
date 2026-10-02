import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import ProfileSection from '@/components/profile/ProfileSection.vue'
import type { AccountFailureReason } from '@/types/auth'

/** Mounts the section with the given feedback and slot content. */
function mountSection(props: { failureReason?: AccountFailureReason | null; successMessage?: string | null } = {}) {
  return mount(ProfileSection, {
    props: {
      headingId: 'sample-heading',
      heading: 'Beispiel',
      description: 'Eine Beschreibung.',
      failureReason: null,
      successMessage: null,
      ...props,
    },
    global: { plugins: [i18n] },
    slots: { default: '<input id="field" />', actions: '<button id="action">Speichern</button>' },
  })
}

beforeEach(() => {
  i18n.global.locale.value = 'de'
})

describe('ProfileSection', () => {
  it('is labelled by its heading and shows heading and description', () => {
    const wrapper = mountSection()

    expect(wrapper.find('section').attributes('aria-labelledby')).toBe('sample-heading')
    expect(wrapper.find('#sample-heading').text()).toBe('Beispiel')
    expect(wrapper.text()).toContain('Eine Beschreibung.')
  })

  it('renders the fields before the feedback and the actions after it', () => {
    const wrapper = mountSection({ failureReason: 'usernameTaken' })

    const html = wrapper.html()

    expect(html.indexOf('id="field"')).toBeLessThan(html.indexOf('bereits vergeben'))
    expect(html.indexOf('bereits vergeben')).toBeLessThan(html.indexOf('id="action"'))
  })

  it('announces a failure as alert and a success as status', () => {
    const wrapper = mountSection({ failureReason: 'currentPasswordIncorrect', successMessage: 'Gespeichert' })

    expect(wrapper.find('[role="alert"]').text()).toBe('Das bisherige Passwort ist falsch.')
    expect(wrapper.find('[role="status"]').text()).toBe('Gespeichert')
  })

  it('translates the failure reason into its message', () => {
    const wrapper = mountSection({ failureReason: 'tooManyRequests' })

    expect(wrapper.find('[role="alert"]').text()).toBe('Zu viele Versuche. Bitte versuchen Sie es in einigen Minuten erneut.')
  })

  it('shows no feedback without messages', () => {
    const wrapper = mountSection()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.find('[role="status"]').exists()).toBe(false)
  })
})
