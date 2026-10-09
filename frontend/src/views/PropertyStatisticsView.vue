<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { usePropertiesStore } from '@/stores/properties'
import { useAppointmentsStore } from '@/stores/appointments'
import PropertyStatisticsList from '@/components/statistics/PropertyStatisticsList.vue'

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const appointmentsStore = useAppointmentsStore()

const hasAppointmentsLoadError = ref(false)

onMounted(async () => {
  propertiesStore.fetchProperties()
  try {
    await appointmentsStore.fetchAppointments()
  } catch {
    hasAppointmentsLoadError.value = true
  }
})
</script>

<template>
  <div class="property-statistics-view">
    <RouterLink :to="{ name: 'statistics' }" class="property-statistics-view__back">
      <i class="pi pi-arrow-left" aria-hidden="true"></i>
      {{ t('statistics.properties.back') }}
    </RouterLink>
    <h1 class="property-statistics-view__heading">{{ t('statistics.properties.heading') }}</h1>
    <p v-if="hasAppointmentsLoadError" class="property-statistics-view__error">
      {{ t('statistics.properties.appointmentsLoadError') }}
    </p>
    <PropertyStatisticsList />
  </div>
</template>

<style scoped src="@/styles/statistics/property-statistics-view.css"></style>
