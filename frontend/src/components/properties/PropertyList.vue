<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePropertiesStore } from '@/stores/properties'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertyAppointmentSummaries } from '@/composables/usePropertyAppointmentSummaries'
import { usePropertyDeleteConfirmation } from '@/composables/usePropertyDeleteConfirmation'
import PropertyCard from '@/components/properties/PropertyCard.vue'
import PropertyCreateCard from '@/components/properties/PropertyCreateCard.vue'
import type { Property } from '@/types/property'

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const appointmentsStore = useAppointmentsStore()
const { getSummaryFor } = usePropertyAppointmentSummaries()
const { confirmDelete } = usePropertyDeleteConfirmation()

onMounted(() => {
  propertiesStore.fetchProperties()
  appointmentsStore.fetchAppointments()
})

function handleDeleteProperty(property: Property) {
  confirmDelete(property, () => propertiesStore.deleteProperty(property.id))
}
</script>

<template>
  <div class="property-list">
    <PropertyCreateCard />

    <p v-if="propertiesStore.hasDeleteError" class="property-list__error">
      {{ t('properties.list.deleteError') }}
    </p>
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
      @edit-property="propertiesStore.openEditDialog"
      @delete-property="handleDeleteProperty"
    />
  </div>
</template>

<style scoped src="@/styles/properties/property-list.css"></style>
