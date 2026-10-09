import { ref } from 'vue'
import axios from 'axios'
import { defineStore } from 'pinia'
import * as optimizationService from '@/services/optimizationService'
import { useAppointmentsStore } from '@/stores/appointments'
import { useAuthStore } from '@/stores/auth'
import { getServerErrorMessage } from '@/utils/serverErrorMessage'
import type { OptimizationProposal, OptimizationRun } from '@/types/optimization'

/** HTTP statuses after which the pending list may be stale (the proposal was decided, expired or is gone, or the decision collided with another change), so it is reloaded from the backend. */
const RELOAD_PENDING_STATUSES = [404, 409]

/** Holds the AI optimization proposals awaiting the user's confirmation, the outcome of the latest run and the state of runs and decisions in flight. */
export const useOptimizationStore = defineStore('optimization', () => {
  const pendingProposals = ref<OptimizationProposal[]>([])
  const lastRun = ref<OptimizationRun | null>(null)
  const isRunning = ref(false)
  const hasLoadError = ref(false)
  const hasRunError = ref(false)
  /** The localized message the backend gave for the last failed run, e.g. a used-up demo run, or null if it gave none. */
  const runErrorMessage = ref<string | null>(null)
  const hasDecisionError = ref(false)
  /** The localized message the backend gave for the last failed decision, e.g. an outdated proposal, or null if it gave none. */
  const decisionErrorMessage = ref<string | null>(null)
  const decidingProposalIds = ref<string[]>([])
  /** Bumped by every fetch, every finished run and reset(), so a list that a newer fetch or a run's result already replaced is discarded. */
  let latestFetchToken = 0
  /** Bumped by reset(), so a run or decision answered only after the account changed never touches the new account's data. */
  let sessionEpoch = 0

  /** Loads every proposal still awaiting a decision, recording whether the request failed. */
  async function fetchPendingProposals() {
    const requestToken = ++latestFetchToken
    try {
      const fetched = await optimizationService.fetchPendingProposals()
      if (requestToken !== latestFetchToken) return
      pendingProposals.value = fetched
      hasLoadError.value = false
    } catch {
      if (requestToken !== latestFetchToken) return
      hasLoadError.value = true
    }
  }

  /** Starts an optimization run whose proposals replace the pending ones, recording the backend's message if it fails and refreshing a demo account's remaining run afterwards. */
  async function startRun(): Promise<boolean> {
    const requestEpoch = sessionEpoch
    isRunning.value = true
    hasRunError.value = false
    runErrorMessage.value = null
    try {
      const run = await optimizationService.startOptimizationRun()
      if (requestEpoch !== sessionEpoch) return false
      latestFetchToken++
      lastRun.value = run
      pendingProposals.value = run.proposals
      return true
    } catch (error) {
      if (requestEpoch !== sessionEpoch) return false
      hasRunError.value = true
      runErrorMessage.value = getServerErrorMessage(error)
      return false
    } finally {
      isRunning.value = false
      await useAuthStore().refreshDemoQuota()
    }
  }

  /** Accepts a proposal, moving its appointment, removes it from the pending list and reloads the appointments so the move shows up everywhere. */
  async function acceptProposal(id: string): Promise<boolean> {
    const accepted = await decide(id, optimizationService.acceptProposal)
    if (accepted) await refreshAppointments()
    return accepted
  }

  /** Reloads the appointments after a move, leaving them as they are if that fails since the calendar and the overview reload them whenever they are opened. */
  async function refreshAppointments() {
    try {
      await useAppointmentsStore().fetchAppointments()
    } catch {
      return
    }
  }

  /** Rejects a proposal and removes it from the pending list. */
  function rejectProposal(id: string): Promise<boolean> {
    return decide(id, optimizationService.rejectProposal)
  }

  /** Sends a decision on one proposal, removing it from the pending list once decided, recording the backend's message if the decision failed and reloading the pending list when the backend reports a conflict or a missing proposal, since only the backend knows whether it is still pending. */
  async function decide(id: string, request: (proposalId: string) => Promise<OptimizationProposal>): Promise<boolean> {
    const requestEpoch = sessionEpoch
    decidingProposalIds.value = [...decidingProposalIds.value, id]
    try {
      await request(id)
      if (requestEpoch !== sessionEpoch) return false
      removePending(id)
      hasDecisionError.value = false
      decisionErrorMessage.value = null
      return true
    } catch (error) {
      if (requestEpoch !== sessionEpoch) return false
      hasDecisionError.value = true
      decisionErrorMessage.value = getServerErrorMessage(error)
      if (axios.isAxiosError(error) && RELOAD_PENDING_STATUSES.includes(error.response?.status ?? 0)) {
        await fetchPendingProposals()
      }
      return false
    } finally {
      decidingProposalIds.value = decidingProposalIds.value.filter((decidingId) => decidingId !== id)
    }
  }

  function removePending(id: string) {
    pendingProposals.value = pendingProposals.value.filter((proposal) => proposal.id !== id)
  }

  /** Returns whether a decision on the given proposal is in flight. */
  function isDeciding(id: string): boolean {
    return decidingProposalIds.value.includes(id)
  }

  /** Forgets every proposal and run state, e.g. when the logged-in account changes. */
  function reset() {
    latestFetchToken++
    sessionEpoch++
    pendingProposals.value = []
    lastRun.value = null
    isRunning.value = false
    hasLoadError.value = false
    hasRunError.value = false
    runErrorMessage.value = null
    hasDecisionError.value = false
    decisionErrorMessage.value = null
    decidingProposalIds.value = []
  }

  return {
    pendingProposals,
    lastRun,
    isRunning,
    hasLoadError,
    hasRunError,
    runErrorMessage,
    hasDecisionError,
    decisionErrorMessage,
    fetchPendingProposals,
    startRun,
    acceptProposal,
    rejectProposal,
    isDeciding,
    reset,
  }
})
