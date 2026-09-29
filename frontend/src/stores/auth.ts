import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authService from '@/services/authService'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertiesStore } from '@/stores/properties'
import type { CurrentUser } from '@/types/auth'

/** Holds the logged-in account and every session transition (restore, login, demo account, logout, expiry). */
export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<CurrentUser | null>(null)
  const isSessionChecked = ref(false)

  const isAuthenticated = computed(() => currentUser.value !== null)
  const isDemoAccount = computed(() => currentUser.value?.demoAccount ?? false)

  /** Looks up an existing session once, e.g. on page load, so a still-valid session skips the login screen. */
  async function restoreSession() {
    try {
      currentUser.value = await authService.fetchCurrentUser()
    } catch {
      currentUser.value = null
    } finally {
      isSessionChecked.value = true
    }
  }

  /** Logs in with the given credentials; rejects with the backend error when they are wrong or rate-limited. */
  async function login(username: string, password: string) {
    const user = await authService.login(username, password)
    startSession(user)
    return user
  }

  /** Creates a demo account and starts its session; rejects with the backend error when rate-limited. */
  async function startDemoSession() {
    const user = await authService.createDemoAccount()
    startSession(user)
    return user
  }

  /** Ends the session on the backend and forgets every account-bound data locally, even if the request fails. */
  async function logout() {
    try {
      await authService.logout()
    } finally {
      clearSession()
    }
  }

  /** Re-reads a demo account's remaining creation limits after it created something; a no-op for regular accounts. */
  async function refreshDemoQuota() {
    if (!isDemoAccount.value) return
    try {
      currentUser.value = await authService.fetchCurrentUser()
    } catch {
      return
    }
  }

  /** Forgets the account and all of its locally cached properties and appointments, e.g. once the session expired. */
  function clearSession() {
    currentUser.value = null
    isSessionChecked.value = true
    useAppointmentsStore().reset()
    usePropertiesStore().reset()
  }

  /** Switches to the given account, dropping any data cached for a previous one. */
  function startSession(user: CurrentUser) {
    useAppointmentsStore().reset()
    usePropertiesStore().reset()
    currentUser.value = user
    isSessionChecked.value = true
  }

  return {
    currentUser,
    isSessionChecked,
    isAuthenticated,
    isDemoAccount,
    restoreSession,
    login,
    startDemoSession,
    logout,
    refreshDemoQuota,
    clearSession,
  }
})
