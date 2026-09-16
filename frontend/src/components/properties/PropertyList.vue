<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePropertiesStore } from '@/stores/properties'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertyAppointmentSummaries } from '@/composables/usePropertyAppointmentSummaries'
import PropertyCard from '@/components/properties/PropertyCard.vue'

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const appointmentsStore = useAppointmentsStore()
const { getSummaryFor } = usePropertyAppointmentSummaries()

onMounted(() => {
  propertiesStore.fetchProperties()
  appointmentsStore.fetchAppointments()
})
</script>

<template>
  <div class="property-list">
    <p v-if="propertiesStore.hasLoadError" class="property-list__error">
      {{ t('properties.list.loadError') }}
    </p>
    <p
      v-else-if="propertiesStore.properties.length === 0"
      class="property-list__empty"
    >
      {{ t('properties.list.empty') }}
    </p>
    <PropertyCard
      v-for="property in propertiesStore.properties"
      :key="property.id"
      :property="property"
      v-bind="getSummaryFor(property.id)"
      @open-appointment="appointmentsStore.openDetail"
    />
  </div>
</template>

<style scoped src="@/styles/properties/property-list.css"></style>
