<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAppointmentMapMarkers } from '@/composables/useAppointmentMapMarkers'
import AppointmentMap from '@/components/map/AppointmentMap.vue'

const { t } = useI18n()
const { markers, hasGeocodingError } = useAppointmentMapMarkers()
</script>

<template>
  <div class="appointment-map-card">
    <div class="appointment-map-card__header">
      <h2 class="appointment-map-card__title">{{ t('overview.map.title') }}</h2>
      <span v-if="hasGeocodingError" class="appointment-map-card__hint">
        {{ t('overview.map.geocodingError') }}
      </span>
    </div>

    <p v-if="markers.length === 0" class="appointment-map-card__empty">
      {{ t('overview.map.empty') }}
    </p>
    <AppointmentMap v-else :markers="markers" />
  </div>
</template>

<style scoped src="@/styles/map/appointment-map-card.css"></style>
