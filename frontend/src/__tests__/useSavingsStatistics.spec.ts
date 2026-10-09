import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { useSavingsStatistics } from '@/composables/useSavingsStatistics'
import * as optimizationService from '@/services/optimizationService'
import { createSavingsStatistics } from '@/__tests__/optimizationFixtures'

vi.mock('@/services/optimizationService')

const weekly = createSavingsStatistics()
const monthly = createSavingsStatistics({ granularity: 'MONTH' })

beforeEach(() => {
  vi.mocked(optimizationService.fetchSavingsStatistics)
    .mockReset()
    .mockImplementation(async (granularity) => (granularity === 'WEEK' ? weekly : monthly))
})

describe('useSavingsStatistics', () => {
  it('loads the weekly savings by default', async () => {
    const { statistics, isLoading, hasLoadError, load } = useSavingsStatistics()

    const loading = load()
    expect(isLoading.value).toBe(true)
    await loading

    expect(optimizationService.fetchSavingsStatistics).toHaveBeenCalledWith('WEEK')
    expect(statistics.value).toEqual(weekly)
    expect(isLoading.value).toBe(false)
    expect(hasLoadError.value).toBe(false)
  })

  it('reloads whenever the granularity changes', async () => {
    const { granularity, statistics, load } = useSavingsStatistics()
    await load()

    granularity.value = 'MONTH'
    await flushPromises()

    expect(optimizationService.fetchSavingsStatistics).toHaveBeenLastCalledWith('MONTH')
    expect(statistics.value).toEqual(monthly)
  })

  it('records a failed load and keeps the last statistics', async () => {
    const { statistics, hasLoadError, load } = useSavingsStatistics()
    await load()
    vi.mocked(optimizationService.fetchSavingsStatistics).mockRejectedValue(new Error('offline'))

    await load()

    expect(hasLoadError.value).toBe(true)
    expect(statistics.value).toEqual(weekly)
  })

  it('ignores an answer that a newer request already superseded', async () => {
    let finishWeekly: (value: typeof weekly) => void = () => {}
    vi.mocked(optimizationService.fetchSavingsStatistics)
      .mockReturnValueOnce(new Promise((resolve) => (finishWeekly = resolve)))
      .mockResolvedValueOnce(monthly)
    const { granularity, statistics, load } = useSavingsStatistics()

    const slowWeekly = load()
    granularity.value = 'MONTH'
    await flushPromises()
    finishWeekly(weekly)
    await slowWeekly

    expect(statistics.value).toEqual(monthly)
  })
})
