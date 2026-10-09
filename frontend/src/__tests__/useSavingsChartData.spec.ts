import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, ref, type Ref } from 'vue'
import { i18n } from '@/i18n'
import { cumulativeSums, useSavingsChartData } from '@/composables/useSavingsChartData'
import { createSavingsStatistics } from '@/__tests__/optimizationFixtures'
import type { SavingsStatistics } from '@/types/optimization'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Runs the composable inside a component so vue-i18n is available, returning its result. */
function useChartDataIn(statistics: Ref<SavingsStatistics | null>) {
  let result: ReturnType<typeof useSavingsChartData> | undefined
  mount(
    defineComponent({
      setup() {
        result = useSavingsChartData(statistics)
        return () => null
      },
    }),
    { global: { plugins: [i18n] } },
  )
  return result!
}

describe('useSavingsChartData', () => {
  it('adds up the values period by period', () => {
    expect(cumulativeSums([1, 0, 2.5])).toEqual([1, 1, 3.5])
    expect(cumulativeSums([])).toEqual([])
  })

  it('charts the cumulative kilometres and driving hours per week with German day-month labels', () => {
    const { chartData } = useChartDataIn(ref(createSavingsStatistics()))

    expect(chartData.value.labels).toEqual(['28.09.', '05.10.'])
    expect(chartData.value.datasets[0]!.label).toBe('Kilometer (kumuliert)')
    expect(chartData.value.datasets[0]!.data).toEqual([10, 12.5])
    expect(chartData.value.datasets[1]!.label).toBe('Fahrzeit in Stunden (kumuliert)')
    expect(chartData.value.datasets[1]!.data).toEqual([0.3, 0.5])
    expect(chartData.value.datasets.map((dataset) => dataset.yAxisID)).toEqual(['distance', 'duration'])
  })

  it('labels monthly points with the short month name and year of the active locale', () => {
    i18n.global.locale.value = 'en'
    const statistics = ref(createSavingsStatistics({
      granularity: 'MONTH',
      periods: [{ periodStart: new Date(2026, 9, 1), savedDistanceMeters: 1000, savedDurationSeconds: 3600, acceptedProposalCount: 1 }],
    }))

    const { chartData } = useChartDataIn(statistics)

    expect(chartData.value.labels).toEqual(['Oct 2026'])
    expect(chartData.value.datasets[0]!.label).toBe('Kilometres (cumulative)')
  })

  it('keeps the chart responsive with a kilometre axis on the left and an hour axis on the right', () => {
    const { chartOptions } = useChartDataIn(ref(createSavingsStatistics()))

    expect(chartOptions.value.responsive).toBe(true)
    expect(chartOptions.value.maintainAspectRatio).toBe(false)
    expect(chartOptions.value.scales.distance.position).toBe('left')
    expect(chartOptions.value.scales.distance.title.text).toBe('km')
    expect(chartOptions.value.scales.duration.position).toBe('right')
    expect(chartOptions.value.scales.duration.title.text).toBe('Std')
  })

  it('draws an empty chart while no statistics are loaded', () => {
    const { chartData } = useChartDataIn(ref(null))

    expect(chartData.value.labels).toEqual([])
    expect(chartData.value.datasets[0]!.data).toEqual([])
  })
})
