<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import SelectButton from 'primevue/selectbutton'
import { useI18n } from 'vue-i18n'
import { useSavingsStatistics } from '@/composables/useSavingsStatistics'
import SavingsSummary from '@/components/statistics/SavingsSummary.vue'
import SavingsLineChart from '@/components/statistics/SavingsLineChart.vue'
import type { SavingsGranularity } from '@/types/optimization'

const { t } = useI18n()
const { granularity, statistics, hasLoadError, load } = useSavingsStatistics()

const granularities: SavingsGranularity[] = ['WEEK', 'MONTH']
const granularityOptions = computed(() =>
  granularities.map((value) => ({ value, label: t(`statistics.savings.granularity.${value}`) })),
)

const hasSavings = computed(() => (statistics.value?.acceptedProposalCount ?? 0) > 0)

onMounted(load)
</script>

<template>
  <div class="savings-statistics-view">
    <RouterLink :to="{ name: 'statistics' }" class="savings-statistics-view__back">
      <i class="pi pi-arrow-left" aria-hidden="true"></i>
      {{ t('statistics.savings.back') }}
    </RouterLink>
    <div class="savings-statistics-view__header">
      <h1 class="savings-statistics-view__heading">{{ t('statistics.savings.heading') }}</h1>
      <SelectButton
        v-model="granularity"
        class="savings-statistics-view__granularity"
        :options="granularityOptions"
        option-label="label"
        option-value="value"
        :allow-empty="false"
        :aria-label="t('statistics.savings.granularity.label')"
      />
    </div>

    <p v-if="hasLoadError" class="savings-statistics-view__error" role="alert">
      {{ t('statistics.savings.loadError') }}
    </p>

    <template v-if="statistics">
      <SavingsSummary :statistics="statistics" />
      <SavingsLineChart v-if="hasSavings" :statistics="statistics" />
      <div v-else class="savings-statistics-view__empty">
        <p>{{ t('statistics.savings.empty') }}</p>
        <RouterLink :to="{ name: 'optimization' }" class="savings-statistics-view__cta">
          <i class="pi pi-sparkles" aria-hidden="true"></i>
          {{ t('statistics.savings.toOptimization') }}
        </RouterLink>
      </div>
    </template>
  </div>
</template>

<style scoped src="@/styles/statistics/savings-statistics-view.css"></style>
