<script setup lang="ts">
import { toRef } from 'vue'
import Chart from 'primevue/chart'
import { useI18n } from 'vue-i18n'
import { useSavingsChartData } from '@/composables/useSavingsChartData'
import type { SavingsStatistics } from '@/types/optimization'

const props = defineProps<{ statistics: SavingsStatistics }>()

const { t } = useI18n()
const { chartData, chartOptions } = useSavingsChartData(toRef(props, 'statistics'))
</script>

<template>
  <figure class="savings-line-chart">
    <figcaption class="savings-line-chart__caption">{{ t('statistics.savings.chart.label') }}</figcaption>
    <div class="savings-line-chart__canvas">
      <Chart class="savings-line-chart__chart" type="line" :data="chartData" :options="chartOptions" />
    </div>
  </figure>
</template>

<style scoped src="@/styles/statistics/savings-line-chart.css"></style>
