import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import ProfilePasswordConfirmDialog from '@/components/profile/ProfilePasswordConfirmDialog.vue'

beforeEach(() => {
  i18n.global.locale.value = 'de'
  document.body.innerHTML = ''
})

afterEach(() => {
  document.body.innerHTML = ''
})

/** Mounts the dialog with the given props; PrimeVue's Dialog teleports its content to document.body one microtask later. */
async function mountDialog(props: { visible?: boolean; isSaving?: boolean; errorMessage?: string | null } = {}) {
  const wrapper = mount(ProfilePasswordConfirmDialog, {
    props: { visible: true, isSaving: false, errorMessage: null, ...props },
    global: { plugins: [i18n, PrimeVue] },
    attachTo: document.body,
  })
  await flushPromises()
  return wrapper
}

/** Finds an element inside the Dialog's teleported content by CSS selector. */
function bodyElement(selector: string): DOMWrapper<HTMLInputElement> {
  return new DOMWrapper(document.body.querySelector(selector) as HTMLInputElement)
}

describe('ProfilePasswordConfirmDialog', () => {
  it('asks for the current password', async () => {
    await mountDialog()

    expect(document.body.textContent).toContain('Passwort bestätigen')
    expect(bodyElement('#profile-confirm-current-password').attributes('type')).toBe('password')
  })

  it('cannot be confirmed before a password was typed', async () => {
    await mountDialog()

    expect(bodyElement('.profile-password-confirm-dialog__submit').attributes('disabled')).toBeDefined()
  })

  it('emits the typed password when confirmed by button or enter key', async () => {
    const wrapper = await mountDialog()

    await bodyElement('#profile-confirm-current-password').setValue('mein passwort')
    await bodyElement('.profile-password-confirm-dialog__submit').trigger('click')
    await bodyElement('#profile-confirm-current-password').trigger('keydown.enter')

    expect(wrapper.emitted('confirm')).toEqual([['mein passwort'], ['mein passwort']])
  })

  it('does not emit anything on enter while the password is empty', async () => {
    const wrapper = await mountDialog()

    await bodyElement('#profile-confirm-current-password').trigger('keydown.enter')

    expect(wrapper.emitted('confirm')).toBeUndefined()
  })

  it('shows the given error message', async () => {
    await mountDialog({ errorMessage: 'Das bisherige Passwort ist falsch.' })

    expect(document.body.querySelector('#profile-confirm-error')?.textContent).toContain(
      'Das bisherige Passwort ist falsch.',
    )
  })

  it('disables confirming while saving', async () => {
    await mountDialog({ isSaving: true })

    expect(bodyElement('.profile-password-confirm-dialog__submit').attributes('disabled')).toBeDefined()
  })

  it('starts with an empty password every time it is opened again', async () => {
    const wrapper = await mountDialog()
    await bodyElement('#profile-confirm-current-password').setValue('altes eingabe')

    await wrapper.setProps({ visible: false })
    await flushPromises()
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect(bodyElement('#profile-confirm-current-password').element.value).toBe('')
  })

  it('closes via the cancel button', async () => {
    const wrapper = await mountDialog()

    await bodyElement('.profile-password-confirm-dialog .profile-settings-dialog__close').trigger('click')

    expect(wrapper.emitted('update:visible')).toEqual([[false]])
  })
})
