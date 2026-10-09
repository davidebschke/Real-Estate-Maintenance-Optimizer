import { ref, watch } from 'vue'
import * as optimizationService from '@/services/optimizationService'
import type { SavingsGranularity, SavingsStatistics } from '@/types/optimization'

/** Loads the AI optimization savings for the selected granularity, reloading whenever it changes and ignoring a response that a newer request already superseded. */
export function useSavingsStatistics(initialGranularity: SavingsGranularity = 'WEEK') {
  const granularity = ref<SavingsGranularity>(initialGranularity)
  const statistics = ref<SavingsStatistics | null>(null)
  const isLoading = ref(false)
  const hasLoadError = ref(false)
  let latestRequestToken = 0

  /** Loads the savings for the current granularity, recording whether the request failed. */
  async function load() {
    const requestToken = ++latestRequestToken
    isLoading.value = true
    try {
      const loaded = await optimizationService.fetchSavingsStatistics(granularity.value)
      if (requestToken !== latestRequestToken) return
      statistics.value = loaded
      hasLoadError.value = false
    } catch {
      if (requestToken !== latestRequestToken) return
      hasLoadError.value = true
    } finally {
      if (requestToken === latestRequestToken) isLoading.value = false
    }
  }

  watch(granularity, load)

  return { granularity, statistics, isLoading, hasLoadError, load }
}
