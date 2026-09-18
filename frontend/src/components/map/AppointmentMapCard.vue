<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppointmentMapMarkers } from '@/composables/useAppointmentMapMarkers'
import { usePropertiesStore } from '@/stores/properties'
import AppointmentMap from '@/components/map/AppointmentMap.vue'

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const { markers, hasGeocodingError } = useAppointmentMapMarkers()

onMounted(() => {
  propertiesStore.fetchProperties()
})
</script>

<template>
  <div class="appointment-map-card card-3d">
    <div v-if="hasGeocodingError" class="appointment-map-card__header">
      <span class="appointment-map-card__hint">
        {{ t('overview.map.geocodingError') }}
      </span>
    </div>

    <p v-if="markers.length === 0" class="appointment-map-card__empty">
      {{ t('overview.map.empty') }}
    </p>
    <AppointmentMap v-else :markers="markers" />
  </div>
</template>

<style scoped src="@/styles/card-3d.css"></style>
<style scoped src="@/styles/map/appointment-map-card.css"></style>
