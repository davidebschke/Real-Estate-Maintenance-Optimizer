<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'
import { formatLocalizedDateTime } from '@/utils/dateFormat'
import type { PropertyAppointmentSummary } from '@/composables/usePropertyAppointmentSummaries'
import type { Property } from '@/types/property'

defineProps<{
  property: Property
  summary: PropertyAppointmentSummary
}>()

defineEmits<{
  'open-appointment': [appointmentId: string]
}>()

const { t } = useI18n()
const { currentLocale } = useLocale()
</script>

<template>
  <tr class="property-statistics-row">
    <th scope="row" class="property-statistics-row__property">
      <span class="property-statistics-row__name">{{ property.name }}</span>
      <span class="property-statistics-row__address">{{ property.address }}</span>
    </th>
    <td class="property-statistics-row__open">{{ summary.openCount }}</td>
    <td class="property-statistics-row__completed">{{ summary.completedCount }}</td>
    <td class="property-statistics-row__next">
      <button
        v-if="summary.nextAppointment"
        type="button"
        class="property-statistics-row__next-link"
        @click="$emit('open-appointment', summary.nextAppointment.id)"
      >
        <span class="property-statistics-row__next-title">{{ summary.nextAppointment.title }}</span>
        <span class="property-statistics-row__next-date">{{
          formatLocalizedDateTime(summary.nextAppointment.start, currentLocale)
        }}</span>
      </button>
      <span v-else class="property-statistics-row__no-next">{{
        t('statistics.properties.noNextAppointment')
      }}</span>
    </td>
  </tr>
</template>

<style scoped src="@/styles/statistics/property-statistics-row.css"></style>
