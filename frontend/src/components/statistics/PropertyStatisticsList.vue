<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { usePropertiesStore } from '@/stores/properties'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertyAppointmentSummaries } from '@/composables/usePropertyAppointmentSummaries'
import PropertyStatisticsRow from '@/components/statistics/PropertyStatisticsRow.vue'

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const appointmentsStore = useAppointmentsStore()
const { getSummaryFor } = usePropertyAppointmentSummaries()
</script>

<template>
  <p v-if="propertiesStore.hasLoadError" class="property-statistics-list__error">
    {{ t('statistics.properties.loadError') }}
  </p>
  <p v-else-if="propertiesStore.properties.length === 0" class="property-statistics-list__empty">
    {{ t('statistics.properties.empty') }}
  </p>
  <div v-else class="property-statistics-list">
    <table class="property-statistics-list__table">
      <thead>
        <tr>
          <th scope="col">{{ t('statistics.properties.columns.property') }}</th>
          <th scope="col">{{ t('statistics.properties.columns.open') }}</th>
          <th scope="col">{{ t('statistics.properties.columns.completed') }}</th>
          <th scope="col">{{ t('statistics.properties.columns.next') }}</th>
        </tr>
      </thead>
      <tbody>
        <PropertyStatisticsRow
          v-for="property in propertiesStore.properties"
          :key="property.id"
          :property="property"
          :summary="getSummaryFor(property.id)"
          @open-appointment="appointmentsStore.openDetail"
        />
      </tbody>
    </table>
  </div>
</template>

<style scoped src="@/styles/statistics/property-statistics-list.css"></style>
