import { computed, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SavingsGranularity, SavingsStatistics } from '@/types/optimization'

const DISTANCE_COLOR = '#3b82f6'
const DURATION_COLOR = '#d4a24c'
const GRID_COLOR = 'rgba(139, 143, 150, 0.2)'
const TEXT_COLOR = '#8b8f96'
const METERS_PER_KILOMETER = 1000
const SECONDS_PER_HOUR = 3600

/** Date format of one point's label: day and month for weeks, short month and year for months. */
const LABEL_FORMATS: Record<SavingsGranularity, Intl.DateTimeFormatOptions> = {
  WEEK: { day: '2-digit', month: '2-digit' },
  MONTH: { month: 'short', year: 'numeric' },
}

/** Returns the running totals of the given values, e.g. the cumulative savings up to each period. */
export function cumulativeSums(values: number[]): number[] {
  let total = 0
  return values.map((value) => {
    total += value
    return total
  })
}

/** Rounds a chart value to one decimal place so tooltips stay readable. */
function roundToTenths(value: number): number {
  return Math.round(value * 10) / 10
}

/** Builds the Chart.js data and options of the savings line chart: cumulative kilometres on the left axis and cumulative driving hours on the right one, one point per period, labelled in the active locale. */
export function useSavingsChartData(statistics: Ref<SavingsStatistics | null>) {
  const { t, locale } = useI18n()

  const chartData = computed(() => {
    const periods = statistics.value?.periods ?? []
    const labelFormat = new Intl.DateTimeFormat(locale.value, LABEL_FORMATS[statistics.value?.granularity ?? 'WEEK'])
    return {
      labels: periods.map((period) => labelFormat.format(period.periodStart)),
      datasets: [
        {
          label: t('statistics.savings.chart.distance'),
          data: cumulativeSums(periods.map((period) => period.savedDistanceMeters / METERS_PER_KILOMETER)).map(roundToTenths),
          borderColor: DISTANCE_COLOR,
          backgroundColor: DISTANCE_COLOR,
          tension: 0.3,
          yAxisID: 'distance',
        },
        {
          label: t('statistics.savings.chart.duration'),
          data: cumulativeSums(periods.map((period) => period.savedDurationSeconds / SECONDS_PER_HOUR)).map(roundToTenths),
          borderColor: DURATION_COLOR,
          backgroundColor: DURATION_COLOR,
          borderDash: [6, 4],
          tension: 0.3,
          yAxisID: 'duration',
        },
      ],
    }
  })

  const chartOptions = computed(() => ({
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { labels: { color: TEXT_COLOR } },
    },
    scales: {
      x: { ticks: { color: TEXT_COLOR }, grid: { color: GRID_COLOR } },
      distance: {
        type: 'linear',
        position: 'left',
        beginAtZero: true,
        title: { display: true, text: t('statistics.savings.chart.distanceAxis'), color: TEXT_COLOR },
        ticks: { color: TEXT_COLOR },
        grid: { color: GRID_COLOR },
      },
      duration: {
        type: 'linear',
        position: 'right',
        beginAtZero: true,
        title: { display: true, text: t('statistics.savings.chart.durationAxis'), color: TEXT_COLOR },
        ticks: { color: TEXT_COLOR },
        grid: { drawOnChartArea: false },
      },
    },
  }))

  return { chartData, chartOptions }
}
