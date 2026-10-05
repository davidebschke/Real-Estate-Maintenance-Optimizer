<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppointmentMapMarkers } from '@/composables/useAppointmentMapMarkers'
import { useAppointmentRoute } from '@/composables/useAppointmentRoute'
import { useRouteMode } from '@/composables/useRouteMode'
import { usePropertiesStore } from '@/stores/properties'
import AppointmentMap from '@/components/map/AppointmentMap.vue'
import RouteModeSwitch from '@/components/map/RouteModeSwitch.vue'

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const { markers, hasGeocodingError, allTodaysAppointmentsCompleted } = useAppointmentMapMarkers()
const { routeMode } = useRouteMode()
const { route, hasRouteError } = useAppointmentRoute(markers)

onMounted(() => {
  propertiesStore.fetchProperties()
})
</script>

<template>
  <div class="appointment-map-card card-3d">
    <div v-if="allTodaysAppointmentsCompleted" class="appointment-map-card__completed">
      <i class="pi pi-check appointment-map-card__completed-icon" aria-hidden="true"></i>
      <p class="appointment-map-card__completed-text">{{ t('overview.map.allCompleted') }}</p>
    </div>
    <template v-else>
      <div v-if="hasGeocodingError || hasRouteError" class="appointment-map-card__header">
        <span v-if="hasGeocodingError" class="appointment-map-card__hint">
          {{ t('overview.map.geocodingError') }}
        </span>
        <span
          v-if="hasRouteError"
          class="appointment-map-card__hint appointment-map-card__route-hint"
        >
          {{ t('overview.map.routeError') }}
        </span>
      </div>

      <RouteModeSwitch v-if="markers.length > 1" v-model="routeMode" />

      <p v-if="markers.length === 0" class="appointment-map-card__empty">
        {{ t('overview.map.empty') }}
      </p>
      <AppointmentMap v-else :markers="markers" :route="route?.geometry ?? null" />
    </template>
  </div>
</template>

<style scoped src="@/styles/card-3d.css"></style>
<style scoped src="@/styles/map/appointment-map-card.css"></style>
