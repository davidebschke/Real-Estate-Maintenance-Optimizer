import { ref } from 'vue'
import axios from 'axios'
import { defineStore } from 'pinia'
import * as optimizationService from '@/services/optimizationService'
import { useAuthStore } from '@/stores/auth'
import { getServerErrorMessage } from '@/utils/serverErrorMessage'
import type { OptimizationProposal, OptimizationRun } from '@/types/optimization'

/** HTTP statuses after which a decided proposal no longer belongs to the pending list: already decided or outdated (409) or gone (404). */
const NO_LONGER_PENDING_STATUSES = [404, 409]

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
  /** Bumped by every fetch, run and reset(), so a response that arrives after a newer one or an account change is discarded. */
  let latestChangeToken = 0

  /** Loads every proposal still awaiting a decision, recording whether the request failed. */
  async function fetchPendingProposals() {
    const requestToken = ++latestChangeToken
    try {
      const fetched = await optimizationService.fetchPendingProposals()
      if (requestToken !== latestChangeToken) return
      pendingProposals.value = fetched
      hasLoadError.value = false
    } catch {
      if (requestToken !== latestChangeToken) return
      hasLoadError.value = true
    }
  }

  /** Starts an optimization run whose proposals replace the pending ones, recording the backend's message if it fails and refreshing a demo account's remaining run afterwards. */
  async function startRun(): Promise<boolean> {
    const requestToken = ++latestChangeToken
    isRunning.value = true
    hasRunError.value = false
    runErrorMessage.value = null
    try {
      const run = await optimizationService.startOptimizationRun()
      if (requestToken !== latestChangeToken) return false
      lastRun.value = run
      pendingProposals.value = run.proposals
      return true
    } catch (error) {
      if (requestToken !== latestChangeToken) return false
      hasRunError.value = true
      runErrorMessage.value = getServerErrorMessage(error)
      return false
    } finally {
      isRunning.value = false
      await useAuthStore().refreshDemoQuota()
    }
  }

  /** Accepts a proposal, moving its appointment, and removes it from the pending list. */
  function acceptProposal(id: string): Promise<boolean> {
    return decide(id, optimizationService.acceptProposal)
  }

  /** Rejects a proposal and removes it from the pending list. */
  function rejectProposal(id: string): Promise<boolean> {
    return decide(id, optimizationService.rejectProposal)
  }

  /** Sends a decision on one proposal, removing it from the pending list once decided or once the backend reports it is no longer pending, and recording the backend's message if the decision failed. */
  async function decide(id: string, request: (proposalId: string) => Promise<OptimizationProposal>): Promise<boolean> {
    const requestToken = latestChangeToken
    decidingProposalIds.value = [...decidingProposalIds.value, id]
    try {
      await request(id)
      if (requestToken !== latestChangeToken) return false
      removePending(id)
      hasDecisionError.value = false
      decisionErrorMessage.value = null
      return true
    } catch (error) {
      if (requestToken !== latestChangeToken) return false
      hasDecisionError.value = true
      decisionErrorMessage.value = getServerErrorMessage(error)
      if (axios.isAxiosError(error) && NO_LONGER_PENDING_STATUSES.includes(error.response?.status ?? 0)) {
        removePending(id)
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
    latestChangeToken++
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
