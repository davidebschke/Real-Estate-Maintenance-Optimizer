import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authService from '@/services/authService'
import * as accountService from '@/services/accountService'
import { useAppointmentsStore } from '@/stores/appointments'
import { useOptimizationStore } from '@/stores/optimization'
import { usePropertiesStore } from '@/stores/properties'
import { useTenantsStore } from '@/stores/tenants'
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

  /** Renames the logged-in account after confirming its current password; rejects with the backend error when the name is taken or invalid or the password is wrong. */
  async function changeUsername(username: string, currentPassword: string) {
    currentUser.value = await accountService.changeUsername(username, currentPassword)
  }

  /** Changes the password; the backend ends all other sessions and keeps this one with a fresh cookie, rejecting with its error when the current password is wrong. */
  async function changePassword(currentPassword: string, newPassword: string) {
    currentUser.value = await accountService.changePassword(currentPassword, newPassword)
  }

  /** Changes the minimum gap kept between appointments of the logged-in account. */
  async function changeAppointmentBuffer(appointmentBufferMinutes: number) {
    currentUser.value = await accountService.changeAppointmentBuffer(appointmentBufferMinutes)
  }

  /** Forgets the account and all of its locally cached properties and appointments, e.g. once the session expired. */
  function clearSession() {
    currentUser.value = null
    isSessionChecked.value = true
    useAppointmentsStore().reset()
    usePropertiesStore().reset()
    useTenantsStore().reset()
    useOptimizationStore().reset()
  }

  /** Switches to the given account, dropping any data cached for a previous one. */
  function startSession(user: CurrentUser) {
    useAppointmentsStore().reset()
    usePropertiesStore().reset()
    useTenantsStore().reset()
    useOptimizationStore().reset()
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
    changeUsername,
    changePassword,
    changeAppointmentBuffer,
    clearSession,
  }
})
