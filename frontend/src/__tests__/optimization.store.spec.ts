import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useOptimizationStore } from '@/stores/optimization'
import { useAuthStore } from '@/stores/auth'
import * as optimizationService from '@/services/optimizationService'
import * as authService from '@/services/authService'
import { httpError } from '@/__tests__/httpError'
import { createProposal } from '@/__tests__/optimizationFixtures'

vi.mock('@/services/optimizationService')
vi.mock('@/services/authService')

const firstProposal = createProposal({ id: 'proposal-1' })
const secondProposal = createProposal({ id: 'proposal-2', appointmentId: 'appointment-2' })

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(optimizationService.fetchPendingProposals).mockReset().mockResolvedValue([firstProposal, secondProposal])
  vi.mocked(optimizationService.startOptimizationRun).mockReset()
  vi.mocked(optimizationService.acceptProposal).mockReset()
  vi.mocked(optimizationService.rejectProposal).mockReset()
  vi.mocked(authService.fetchCurrentUser).mockReset()
})

describe('optimization store', () => {
  it('loads the pending proposals', async () => {
    const store = useOptimizationStore()

    await store.fetchPendingProposals()

    expect(store.pendingProposals.map((proposal) => proposal.id)).toEqual(['proposal-1', 'proposal-2'])
    expect(store.hasLoadError).toBe(false)
  })

  it('records a failed load without dropping the proposals it already has', async () => {
    const store = useOptimizationStore()
    await store.fetchPendingProposals()
    vi.mocked(optimizationService.fetchPendingProposals).mockRejectedValue(new Error('offline'))

    await store.fetchPendingProposals()

    expect(store.hasLoadError).toBe(true)
    expect(store.pendingProposals).toHaveLength(2)
  })

  it('replaces the pending proposals with those of a new run and remembers the run', async () => {
    vi.mocked(optimizationService.startOptimizationRun).mockResolvedValue({
      runId: 'run-1',
      createdAt: new Date(),
      analyzedAppointmentCount: 8,
      candidateCount: 3,
      proposals: [secondProposal],
    })
    const store = useOptimizationStore()
    await store.fetchPendingProposals()

    const started = await store.startRun()

    expect(started).toBe(true)
    expect(store.pendingProposals).toEqual([secondProposal])
    expect(store.lastRun?.analyzedAppointmentCount).toBe(8)
    expect(store.isRunning).toBe(false)
  })

  it('records the backend message of a failed run and refreshes the demo run afterwards', async () => {
    vi.mocked(optimizationService.startOptimizationRun).mockRejectedValue(
      httpError(403, { message: 'Ein Demo-Account kann die KI-Optimierung nur einmal nutzen.' }),
    )
    const authStore = useAuthStore()
    const refreshDemoQuota = vi.spyOn(authStore, 'refreshDemoQuota')
    const store = useOptimizationStore()

    const started = await store.startRun()

    expect(started).toBe(false)
    expect(store.hasRunError).toBe(true)
    expect(store.runErrorMessage).toBe('Ein Demo-Account kann die KI-Optimierung nur einmal nutzen.')
    expect(store.isRunning).toBe(false)
    expect(refreshDemoQuota).toHaveBeenCalledTimes(1)
  })

  it('marks the store as running while a run is in flight', async () => {
    let finishRun: (value: Awaited<ReturnType<typeof optimizationService.startOptimizationRun>>) => void = () => {}
    vi.mocked(optimizationService.startOptimizationRun).mockReturnValue(new Promise((resolve) => (finishRun = resolve)))
    const store = useOptimizationStore()

    const running = store.startRun()
    expect(store.isRunning).toBe(true)
    finishRun({ runId: 'run-1', createdAt: new Date(), analyzedAppointmentCount: 0, candidateCount: 0, proposals: [] })
    await running

    expect(store.isRunning).toBe(false)
  })

  it('removes an accepted or rejected proposal from the pending list', async () => {
    vi.mocked(optimizationService.acceptProposal).mockResolvedValue({ ...firstProposal, status: 'ACCEPTED' })
    vi.mocked(optimizationService.rejectProposal).mockResolvedValue({ ...secondProposal, status: 'REJECTED' })
    const store = useOptimizationStore()
    await store.fetchPendingProposals()

    expect(await store.acceptProposal('proposal-1')).toBe(true)
    expect(await store.rejectProposal('proposal-2')).toBe(true)

    expect(optimizationService.acceptProposal).toHaveBeenCalledWith('proposal-1')
    expect(optimizationService.rejectProposal).toHaveBeenCalledWith('proposal-2')
    expect(store.pendingProposals).toEqual([])
    expect(store.hasDecisionError).toBe(false)
  })

  it('reports a decision in flight for its proposal only', async () => {
    let finishDecision: (value: typeof firstProposal) => void = () => {}
    vi.mocked(optimizationService.acceptProposal).mockReturnValue(new Promise((resolve) => (finishDecision = resolve)))
    const store = useOptimizationStore()
    await store.fetchPendingProposals()

    const deciding = store.acceptProposal('proposal-1')
    expect(store.isDeciding('proposal-1')).toBe(true)
    expect(store.isDeciding('proposal-2')).toBe(false)
    finishDecision(firstProposal)
    await deciding

    expect(store.isDeciding('proposal-1')).toBe(false)
  })

  it('reloads the pending list from the backend after a conflict or a missing proposal, keeping the backend message', async () => {
    vi.mocked(optimizationService.acceptProposal).mockRejectedValue(
      httpError(409, { message: 'Dieser Vorschlag ist veraltet.' }),
    )
    vi.mocked(optimizationService.rejectProposal).mockRejectedValue(httpError(404, { message: 'Nicht gefunden.' }))
    const store = useOptimizationStore()
    await store.fetchPendingProposals()
    vi.mocked(optimizationService.fetchPendingProposals).mockResolvedValue([secondProposal])

    expect(await store.acceptProposal('proposal-1')).toBe(false)

    expect(store.decisionErrorMessage).toBe('Dieser Vorschlag ist veraltet.')
    expect(optimizationService.fetchPendingProposals).toHaveBeenCalledTimes(2)
    expect(store.pendingProposals.map((proposal) => proposal.id)).toEqual(['proposal-2'])

    vi.mocked(optimizationService.fetchPendingProposals).mockResolvedValue([])
    await store.rejectProposal('proposal-2')

    expect(store.pendingProposals).toEqual([])
  })

  it('keeps the proposal of a decision that failed for another reason without reloading', async () => {
    vi.mocked(optimizationService.acceptProposal).mockRejectedValue(httpError(503, {}))
    const store = useOptimizationStore()
    await store.fetchPendingProposals()

    expect(await store.acceptProposal('proposal-1')).toBe(false)

    expect(store.hasDecisionError).toBe(true)
    expect(store.decisionErrorMessage).toBeNull()
    expect(optimizationService.fetchPendingProposals).toHaveBeenCalledTimes(1)
    expect(store.pendingProposals.map((proposal) => proposal.id)).toEqual(['proposal-1', 'proposal-2'])
  })

  it('keeps the result of a run even when the pending list was reloaded while the run was in flight', async () => {
    let finishRun: (value: Awaited<ReturnType<typeof optimizationService.startOptimizationRun>>) => void = () => {}
    vi.mocked(optimizationService.startOptimizationRun).mockReturnValue(new Promise((resolve) => (finishRun = resolve)))
    let finishLoad: (value: (typeof firstProposal)[]) => void = () => {}
    const store = useOptimizationStore()

    const running = store.startRun()
    vi.mocked(optimizationService.fetchPendingProposals).mockReturnValue(new Promise((resolve) => (finishLoad = resolve)))
    const loading = store.fetchPendingProposals()
    finishRun({ runId: 'run-2', createdAt: new Date(), analyzedAppointmentCount: 3, candidateCount: 1, proposals: [secondProposal] })
    await running
    finishLoad([firstProposal])
    await loading

    expect(store.lastRun?.runId).toBe('run-2')
    expect(store.pendingProposals).toEqual([secondProposal])
  })

  it('forgets everything on reset and ignores a load that finishes afterwards', async () => {
    let finishLoad: (value: (typeof firstProposal)[]) => void = () => {}
    const store = useOptimizationStore()
    await store.fetchPendingProposals()
    vi.mocked(optimizationService.fetchPendingProposals).mockReturnValue(new Promise((resolve) => (finishLoad = resolve)))

    const loading = store.fetchPendingProposals()
    store.reset()
    finishLoad([firstProposal])
    await loading

    expect(store.pendingProposals).toEqual([])
    expect(store.lastRun).toBeNull()
    expect(store.hasRunError).toBe(false)
  })

  it('is reset when the session of the logged-in account ends', async () => {
    const store = useOptimizationStore()
    await store.fetchPendingProposals()

    useAuthStore().clearSession()

    expect(store.pendingProposals).toEqual([])
  })
})
