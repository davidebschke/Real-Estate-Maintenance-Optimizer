<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'
import { formatKilometers, formatTravelDuration } from '@/utils/travelFormat'
import type { SavingsStatistics } from '@/types/optimization'

const props = defineProps<{ statistics: SavingsStatistics }>()

const { t } = useI18n()
const { currentLocale } = useLocale()

const tiles = computed(() => [
  {
    key: 'distance',
    icon: 'pi-car',
    label: t('statistics.savings.totals.distance'),
    value: `${formatKilometers(props.statistics.totalSavedDistanceMeters, currentLocale.value)} km`,
  },
  {
    key: 'duration',
    icon: 'pi-clock',
    label: t('statistics.savings.totals.duration'),
    value: formatTravelDuration(props.statistics.totalSavedDurationSeconds, currentLocale.value),
  },
  {
    key: 'accepted',
    icon: 'pi-check-circle',
    label: t('statistics.savings.totals.accepted'),
    value: String(props.statistics.acceptedProposalCount),
  },
])
</script>

<template>
  <dl class="savings-summary">
    <div v-for="tile in tiles" :key="tile.key" class="savings-summary__tile" :data-tile="tile.key">
      <i class="pi savings-summary__icon" :class="tile.icon" aria-hidden="true"></i>
      <dt class="savings-summary__label">{{ tile.label }}</dt>
      <dd class="savings-summary__value">{{ tile.value }}</dd>
    </div>
  </dl>
</template>

<style scoped src="@/styles/statistics/savings-summary.css"></style>
