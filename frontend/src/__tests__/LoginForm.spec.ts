import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import { AxiosError, AxiosHeaders } from 'axios'
import { i18n } from '@/i18n'
import LoginForm from '@/components/auth/LoginForm.vue'
import * as authService from '@/services/authService'

vi.mock('@/services/authService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/authService')>()
  return { ...actual, login: vi.fn<typeof actual.login>() }
})

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
  vi.mocked(authService.login).mockReset()
  i18n.global.locale.value = 'de'
})

/** Mounts the login form with the plugins it needs. */
function mountForm() {
  return mount(LoginForm, { global: { plugins: [i18n, PrimeVue] } })
}

type FormWrapper = ReturnType<typeof mountForm>

/** Enters the given credentials. */
async function enterCredentials(wrapper: FormWrapper, username: string, password: string) {
  await wrapper.find('#login-username').setValue(username)
  await wrapper.find('#login-password').setValue(password)
}

/** The form's submit button. */
function submitButton(wrapper: FormWrapper) {
  return wrapper.findComponent(Button)
}

describe('LoginForm', () => {
  it('labels both fields and supports password managers', () => {
    const wrapper = mountForm()

    expect(wrapper.find('label[for="login-username"]').text()).toBe('Benutzername')
    expect(wrapper.find('label[for="login-password"]').text()).toBe('Passwort')
    expect(wrapper.find('#login-username').attributes('autocomplete')).toBe('username')
    expect(wrapper.find('#login-password').attributes('autocomplete')).toBe('current-password')
    expect(wrapper.find('#login-password').attributes('type')).toBe('password')
  })

  it('keeps submit disabled until both username and password are entered', async () => {
    const wrapper = mountForm()
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await enterCredentials(wrapper, '   ', 'geheim')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await enterCredentials(wrapper, 'debschke', 'geheim')
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('logs in with the trimmed username, turns green and reports the success', async () => {
    vi.mocked(authService.login).mockResolvedValue({
      username: 'debschke',
      displayName: 'David Ebschke',
      demoAccount: false,
      expiresAt: null,
      remainingPropertyCreations: null,
      remainingAppointmentCreations: null,
    })
    const wrapper = mountForm()
    await enterCredentials(wrapper, ' debschke ', 'geheim')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(authService.login).toHaveBeenCalledWith('debschke', 'geheim')
    expect(submitButton(wrapper).classes()).toContain('login-form__submit--success')
    expect(submitButton(wrapper).text()).toBe('Angemeldet')
    expect(wrapper.emitted('authenticated')).toHaveLength(1)
    expect((wrapper.find('#login-password').element as HTMLInputElement).value).toBe('')
  })

  it('shows a wrong-credentials error, clears only the password and marks both fields invalid', async () => {
    vi.mocked(authService.login).mockRejectedValue(httpError(401))
    const wrapper = mountForm()
    await enterCredentials(wrapper, 'debschke', 'falsch')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe('Benutzername oder Passwort ist falsch.')
    expect((wrapper.find('#login-username').element as HTMLInputElement).value).toBe('debschke')
    expect((wrapper.find('#login-password').element as HTMLInputElement).value).toBe('')
    expect(wrapper.find('#login-username').classes()).toContain('p-invalid')
    expect(wrapper.emitted('authenticated')).toBeUndefined()
  })

  it('shows the rate-limit error after too many failed attempts', async () => {
    vi.mocked(authService.login).mockRejectedValue(httpError(429))
    const wrapper = mountForm()
    await enterCredentials(wrapper, 'debschke', 'falsch')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('Zu viele fehlgeschlagene Anmeldeversuche')
  })

  it('shows a generic error when the backend is unreachable', async () => {
    vi.mocked(authService.login).mockRejectedValue(new Error('Network Error'))
    const wrapper = mountForm()
    await enterCredentials(wrapper, 'debschke', 'geheim')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe(
      'Die Anmeldung ist fehlgeschlagen. Bitte versuchen Sie es erneut.',
    )
  })

  it('ignores a second submit while the first login is still running', async () => {
    let resolveLogin: (value: never) => void = () => {}
    vi.mocked(authService.login).mockReturnValue(new Promise((resolve) => (resolveLogin = resolve)))
    const wrapper = mountForm()
    await enterCredentials(wrapper, 'debschke', 'geheim')

    await wrapper.find('form').trigger('submit')
    await wrapper.find('form').trigger('submit')
    resolveLogin(undefined as never)
    await flushPromises()

    expect(authService.login).toHaveBeenCalledOnce()
  })

  it('limits username and password to what the backend accepts', () => {
    const wrapper = mountForm()

    expect(wrapper.find('#login-username').attributes('maxlength')).toBe('50')
    expect(wrapper.find('#login-password').attributes('maxlength')).toBe('128')
  })
})
