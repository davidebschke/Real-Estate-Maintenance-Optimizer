import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import OptimizationLaunchCard from '@/components/optimization/OptimizationLaunchCard.vue'
import { useOptimizationStore } from '@/stores/optimization'
import { useAuthStore } from '@/stores/auth'
import * as optimizationService from '@/services/optimizationService'
import * as authService from '@/services/authService'
import { createProposal } from '@/__tests__/optimizationFixtures'
import type { CurrentUser } from '@/types/auth'

vi.mock('@/services/optimizationService')
vi.mock('@/services/authService')

/** Logs in a demo account with the given number of remaining AI runs directly in the store. */
function logInDemoAccount(remainingAiOptimizations: number): CurrentUser {
  const user: CurrentUser = {
    username: 'demo-1',
    displayName: 'Demo',
    demoAccount: true,
    expiresAt: new Date(),
    remainingPropertyCreations: 3,
    remainingAppointmentCreations: 3,
    remainingTenantCreations: 10,
    remainingAiOptimizations,
    appointmentBufferMinutes: 5,
  }
  useAuthStore().currentUser = user
  return user
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(optimizationService.startOptimizationRun).mockReset().mockResolvedValue({
    runId: 'run-1',
    createdAt: new Date(),
    analyzedAppointmentCount: 12,
    candidateCount: 4,
    proposals: [createProposal()],
  })
  vi.mocked(authService.fetchCurrentUser).mockReset()
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

function mountCard() {
  return mount(OptimizationLaunchCard, { global: { plugins: [PrimeVue, i18n] } })
}

describe('OptimizationLaunchCard', () => {
  it('explains the planning rules with the configured lead time, horizon and recurring shift', () => {
    const rules = mountCard().findAll('.optimization-launch-card__rule').map((node) => node.text())

    expect(rules).toEqual([
      'Verschoben werden nur Termine ab 28 Tagen in der Zukunft, im Blick behält die KI die nächsten 365 Tage.',
      'Unverschiebbare Termine und erledigte Termine bleiben unangetastet.',
      'Wiederkehrende Termine werden höchstens 14 Tage verschoben.',
      'Jeder Vorschlag lässt genug Fahrzeit und Puffer zum vorherigen und zum nächsten Termin.',
    ])
  })

  it('starts a run and summarizes it', async () => {
    const wrapper = mountCard()

    await wrapper.find('.optimization-launch-card__start').trigger('click')
    await flushPromises()

    expect(optimizationService.startOptimizationRun).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.optimization-launch-card__summary').text()).toBe(
      'Letzter Lauf: 12 Termine geprüft, 4 mögliche Verschiebungen bewertet, 1 Vorschläge.',
    )
  })

  it('says so when a run found nothing worth moving', async () => {
    vi.mocked(optimizationService.startOptimizationRun).mockResolvedValue({
      runId: 'run-2',
      createdAt: new Date(),
      analyzedAppointmentCount: 3,
      candidateCount: 0,
      proposals: [],
    })
    const wrapper = mountCard()

    await wrapper.find('.optimization-launch-card__start').trigger('click')
    await flushPromises()

    expect(wrapper.find('.optimization-launch-card__summary').text()).toContain(
      'Die KI hat keine Verschiebung gefunden, die Fahrzeit oder Kilometer spart.',
    )
  })

  it('shows the running state while the AI plans', async () => {
    const wrapper = mountCard()
    useOptimizationStore().isRunning = true
    await flushPromises()

    const start = wrapper.find('.optimization-launch-card__start')
    expect(start.text()).toContain('Die KI plant Ihre Termine …')
    expect(start.attributes('disabled')).toBeDefined()
  })

  it('shows the backend message of a failed run or a generic one', async () => {
    const wrapper = mountCard()
    const store = useOptimizationStore()
    store.hasRunError = true
    await flushPromises()
    expect(wrapper.find('.optimization-launch-card__error').text()).toBe('Die Optimierung konnte nicht gestartet werden.')

    store.runErrorMessage = 'Die KI-Optimierung ist auf diesem Server nicht aktiviert.'
    await flushPromises()
    expect(wrapper.find('.optimization-launch-card__error').text()).toBe(
      'Die KI-Optimierung ist auf diesem Server nicht aktiviert.',
    )
  })

  it('tells a demo account about its single run and blocks the button once it is used', async () => {
    logInDemoAccount(1)
    const wrapper = mountCard()
    expect(wrapper.find('.demo-quota-hint').text()).toBe('Demo-Account: Sie können die KI-Optimierung einmal starten.')
    expect(wrapper.find('.optimization-launch-card__start').attributes('disabled')).toBeUndefined()

    logInDemoAccount(0)
    await flushPromises()

    expect(wrapper.find('.demo-quota-hint').text()).toBe('Demo-Account: Sie haben die KI-Optimierung bereits genutzt.')
    expect(wrapper.find('.optimization-launch-card__start').attributes('disabled')).toBeDefined()
  })

  it('renders the English texts when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mountCard()

    expect(wrapper.find('.optimization-launch-card__title').text()).toBe('How the AI plans')
    expect(wrapper.find('.optimization-launch-card__start').text()).toContain('Optimize appointments')
  })
})
