import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import InputNumber from 'primevue/inputnumber'
import { AxiosError, AxiosHeaders } from 'axios'
import { i18n } from '@/i18n'
import ProfileSettingsDialog from '@/components/profile/ProfileSettingsDialog.vue'
import { useAuthStore } from '@/stores/auth'
import * as accountService from '@/services/accountService'
import type { CurrentUser } from '@/types/auth'

vi.mock('@/services/accountService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/accountService')>()
  return {
    ...actual,
    changeUsername: vi.fn<typeof actual.changeUsername>(),
    changePassword: vi.fn<typeof actual.changePassword>(),
    changeAppointmentBuffer: vi.fn<typeof actual.changeAppointmentBuffer>(),
  }
})

const baseUser: CurrentUser = {
  username: 'debschke',
  displayName: 'David Ebschke',
  demoAccount: false,
  expiresAt: null,
  remainingPropertyCreations: null,
  remainingAppointmentCreations: null,
  appointmentBufferMinutes: 15,
}

/** Builds the axios error a request rejects with for the given HTTP status. */
function httpError(status: number): AxiosError {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data: {},
  })
}

beforeEach(() => {
  setActivePinia(createPinia())
  useAuthStore().currentUser = { ...baseUser }
  vi.mocked(accountService.changeUsername).mockReset()
  vi.mocked(accountService.changePassword).mockReset()
  vi.mocked(accountService.changeAppointmentBuffer).mockReset()
  i18n.global.locale.value = 'de'
  document.body.innerHTML = ''
})

afterEach(() => {
  document.body.innerHTML = ''
})

/** Mounts the dialog already open; PrimeVue's Dialog teleports its content to document.body one microtask later. */
async function mountDialog() {
  const wrapper = mount(ProfileSettingsDialog, {
    props: { visible: true },
    global: { plugins: [i18n, PrimeVue] },
    attachTo: document.body,
  })
  await flushPromises()
  return wrapper
}

/** Finds an input inside the Dialog's teleported content by CSS selector. */
function bodyInput(selector: string): DOMWrapper<HTMLInputElement> {
  return new DOMWrapper(document.body.querySelector(selector) as HTMLInputElement)
}

/** Finds the submit button of the section with the given heading id. */
function sectionButton(headingId: string): DOMWrapper<HTMLButtonElement> {
  const section = document.body.querySelector(`#${headingId}`)?.closest('section')
  return new DOMWrapper(section?.querySelector('button') as HTMLButtonElement)
}

describe('ProfileSettingsDialog', () => {
  it('shows the three sections for username, password and appointment buffer', async () => {
    await mountDialog()

    expect(document.body.textContent).toContain('Profil & Einstellungen')
    expect(document.body.querySelector('#profile-username-heading')).not.toBeNull()
    expect(document.body.querySelector('#profile-password-heading')).not.toBeNull()
    expect(document.body.querySelector('#profile-buffer-heading')).not.toBeNull()
  })

  it('closes via the close button', async () => {
    const wrapper = await mountDialog()

    await new DOMWrapper(document.body.querySelector('.profile-settings-dialog__close') as Element).trigger('click')

    expect(wrapper.emitted('update:visible')).toEqual([[false]])
  })

  describe('username', () => {
    it('is prefilled with the current username and cannot be saved unchanged', async () => {
      await mountDialog()

      expect(bodyInput('#profile-username').element.value).toBe('debschke')
      expect(sectionButton('profile-username-heading').attributes('disabled')).toBeDefined()
    })

    it('flags an invalid username after the field was left and blocks saving', async () => {
      await mountDialog()

      await bodyInput('#profile-username').setValue('a b')
      await bodyInput('#profile-username').trigger('blur')

      expect(document.body.textContent).toContain('Bitte 3 bis 50 Zeichen verwenden')
      expect(sectionButton('profile-username-heading').attributes('disabled')).toBeDefined()
    })

    /** Types the given password into the confirmation dialog and confirms it. */
    async function confirmWithPassword(password: string) {
      await bodyInput('#profile-confirm-current-password').setValue(password)
      await new DOMWrapper(
        document.body.querySelector('.profile-password-confirm-dialog__submit') as HTMLButtonElement,
      ).trigger('click')
      await flushPromises()
    }

    it('asks for the current password when saving and does not save before it was confirmed', async () => {
      await mountDialog()

      await bodyInput('#profile-username').setValue('neuer-name')
      await sectionButton('profile-username-heading').trigger('click')
      await flushPromises()

      expect(document.body.querySelector('.profile-password-confirm-dialog')).not.toBeNull()
      expect(document.body.textContent).toContain('Bitte geben Sie zur Bestätigung Ihr aktuelles Passwort ein.')
      expect(accountService.changeUsername).not.toHaveBeenCalled()
    })

    it('cannot confirm without a typed password', async () => {
      await mountDialog()
      await bodyInput('#profile-username').setValue('neuer-name')
      await sectionButton('profile-username-heading').trigger('click')
      await flushPromises()

      const confirmButton = document.body.querySelector(
        '.profile-password-confirm-dialog__submit',
      ) as HTMLButtonElement

      expect(confirmButton.disabled).toBe(true)
    })

    it('saves the trimmed username with the confirmed password and reports success', async () => {
      vi.mocked(accountService.changeUsername).mockResolvedValue({ ...baseUser, username: 'neuer-name' })
      await mountDialog()

      await bodyInput('#profile-username').setValue('  Neuer-Name ')
      await sectionButton('profile-username-heading').trigger('click')
      await flushPromises()
      await confirmWithPassword('mein passwort')

      expect(accountService.changeUsername).toHaveBeenCalledWith('Neuer-Name', 'mein passwort')
      expect(useAuthStore().currentUser?.username).toBe('neuer-name')
      expect(bodyInput('#profile-username').element.value).toBe('neuer-name')
      expect(document.body.textContent).toContain('Der Benutzername wurde geändert.')
      expect(document.body.querySelector('.profile-password-confirm-dialog')).toBeNull()
    })

    it('keeps the confirmation open with an error when the password is wrong', async () => {
      vi.mocked(accountService.changeUsername).mockRejectedValue(httpError(422))
      await mountDialog()

      await bodyInput('#profile-username').setValue('neuer-name')
      await sectionButton('profile-username-heading').trigger('click')
      await flushPromises()
      await confirmWithPassword('falsches passwort')

      expect(document.body.querySelector('.profile-password-confirm-dialog')).not.toBeNull()
      expect(document.body.textContent).toContain('Das bisherige Passwort ist falsch.')
      expect(useAuthStore().currentUser?.username).toBe('debschke')
    })

    it('does not rename when the confirmation is cancelled', async () => {
      await mountDialog()
      await bodyInput('#profile-username').setValue('neuer-name')
      await sectionButton('profile-username-heading').trigger('click')
      await flushPromises()

      const cancelButton = document.body.querySelector(
        '.profile-password-confirm-dialog .profile-settings-dialog__close',
      ) as HTMLButtonElement
      await new DOMWrapper(cancelButton).trigger('click')
      await flushPromises()

      expect(accountService.changeUsername).not.toHaveBeenCalled()
      expect(document.body.querySelector('.profile-password-confirm-dialog')).toBeNull()
    })

    it('closes the confirmation and tells the user when the username is already taken', async () => {
      vi.mocked(accountService.changeUsername).mockRejectedValue(httpError(409))
      await mountDialog()

      await bodyInput('#profile-username').setValue('vergeben')
      await sectionButton('profile-username-heading').trigger('click')
      await flushPromises()
      await confirmWithPassword('mein passwort')

      expect(document.body.querySelector('.profile-password-confirm-dialog')).toBeNull()
      expect(document.body.textContent).toContain('Dieser Benutzername ist bereits vergeben.')
      expect(useAuthStore().currentUser?.username).toBe('debschke')
    })
  })

  describe('password', () => {
    /** Fills the three password fields. */
    async function fillPasswords(current: string, next: string, confirmation: string) {
      await bodyInput('#profile-current-password').setValue(current)
      await bodyInput('#profile-new-password').setValue(next)
      await bodyInput('#profile-confirm-password').setValue(confirmation)
    }

    it('cannot be saved while the fields are empty', async () => {
      await mountDialog()

      expect(sectionButton('profile-password-heading').attributes('disabled')).toBeDefined()
    })

    it('flags a too short new password after the field was left', async () => {
      await mountDialog()

      await fillPasswords('altes passwort', 'kurz', 'kurz')
      await bodyInput('#profile-new-password').trigger('blur')

      expect(document.body.textContent).toContain('mindestens 8 Zeichen')
      expect(sectionButton('profile-password-heading').attributes('disabled')).toBeDefined()
    })

    it('flags a new password equal to the current one', async () => {
      await mountDialog()

      await fillPasswords('gleiches passwort', 'gleiches passwort', 'gleiches passwort')
      await bodyInput('#profile-new-password').trigger('blur')

      expect(document.body.textContent).toContain('muss sich vom bisherigen unterscheiden')
    })

    it('flags a repeated password that does not match', async () => {
      await mountDialog()

      await fillPasswords('altes passwort', 'neues passwort', 'anderes passwort')
      await bodyInput('#profile-confirm-password').trigger('blur')

      expect(document.body.textContent).toContain('Die Passwörter stimmen nicht überein.')
      expect(sectionButton('profile-password-heading').attributes('disabled')).toBeDefined()
    })

    it('changes the password, clears the fields and reports success', async () => {
      vi.mocked(accountService.changePassword).mockResolvedValue({ ...baseUser })
      await mountDialog()

      await fillPasswords('altes passwort', 'neues passwort', 'neues passwort')
      await sectionButton('profile-password-heading').trigger('click')
      await flushPromises()

      expect(accountService.changePassword).toHaveBeenCalledWith('altes passwort', 'neues passwort')
      expect(bodyInput('#profile-current-password').element.value).toBe('')
      expect(bodyInput('#profile-new-password').element.value).toBe('')
      expect(bodyInput('#profile-confirm-password').element.value).toBe('')
      expect(document.body.textContent).toContain('Das Passwort wurde geändert.')
    })

    it('shows that the current password is wrong and keeps the entered values', async () => {
      vi.mocked(accountService.changePassword).mockRejectedValue(httpError(422))
      await mountDialog()

      await fillPasswords('falsches passwort', 'neues passwort', 'neues passwort')
      await sectionButton('profile-password-heading').trigger('click')
      await flushPromises()

      expect(document.body.textContent).toContain('Das bisherige Passwort ist falsch.')
      expect(bodyInput('#profile-new-password').element.value).toBe('neues passwort')
    })
  })

  describe('appointment buffer', () => {
    it('is prefilled with the current buffer and cannot be saved unchanged', async () => {
      await mountDialog()

      expect(bodyInput('#profile-buffer').element.value).toContain('15')
      expect(sectionButton('profile-buffer-heading').attributes('disabled')).toBeDefined()
    })

    it('saves a changed buffer and reports success', async () => {
      vi.mocked(accountService.changeAppointmentBuffer).mockResolvedValue({
        ...baseUser,
        appointmentBufferMinutes: 45,
      })
      const wrapper = await mountDialog()

      wrapper.findComponent(InputNumber).vm.$emit('update:modelValue', 45)
      await flushPromises()
      await sectionButton('profile-buffer-heading').trigger('click')
      await flushPromises()

      expect(accountService.changeAppointmentBuffer).toHaveBeenCalledWith(45)
      expect(useAuthStore().currentUser?.appointmentBufferMinutes).toBe(45)
      expect(document.body.textContent).toContain('Die Pufferzeit wurde gespeichert.')
    })

    it('flags an empty buffer after the field was left and blocks saving', async () => {
      const wrapper = await mountDialog()

      wrapper.findComponent(InputNumber).vm.$emit('update:modelValue', null)
      await flushPromises()
      await bodyInput('#profile-buffer').trigger('blur')

      expect(document.body.textContent).toContain('Bitte eine ganze Zahl von 0 bis 1440 eingeben.')
      expect(sectionButton('profile-buffer-heading').attributes('disabled')).toBeDefined()
    })

    it('shows a generic error when saving fails', async () => {
      vi.mocked(accountService.changeAppointmentBuffer).mockRejectedValue(httpError(500))
      const wrapper = await mountDialog()

      wrapper.findComponent(InputNumber).vm.$emit('update:modelValue', 0)
      await flushPromises()
      await sectionButton('profile-buffer-heading').trigger('click')
      await flushPromises()

      expect(document.body.textContent).toContain('Die Änderung konnte nicht gespeichert werden.')
      expect(useAuthStore().currentUser?.appointmentBufferMinutes).toBe(15)
    })
  })
})
