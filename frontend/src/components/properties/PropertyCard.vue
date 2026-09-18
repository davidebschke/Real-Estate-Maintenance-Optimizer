<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'
import { useCardTilt } from '@/composables/useCardTilt'
import type { Property } from '@/types/property'
import type { Appointment } from '@/types/appointment'

const props = defineProps<{
  property: Property
  openCount: number
  completedCount: number
  nextAppointment: Appointment | null
}>()

defineEmits<{ 'open-appointment': [appointmentId: string] }>()

const { t } = useI18n()
const { currentLocale } = useLocale()
const { tiltStyle, onPointerMove, onPointerLeave } = useCardTilt()

const nextAppointmentLabel = computed(() => {
  if (!props.nextAppointment) return ''
  const formatter = new Intl.DateTimeFormat(currentLocale.value, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
  return formatter.format(props.nextAppointment.start)
})
</script>

<template>
  <div
    class="property-card card-3d card-3d--interactive"
    :style="tiltStyle"
    @pointermove="onPointerMove"
    @pointerleave="onPointerLeave"
  >
    <div class="property-card__header">
      <i class="pi property-card__icon" :class="property.icon" aria-hidden="true"></i>
      <div class="property-card__heading">
        <h3 class="property-card__name">{{ property.name }}</h3>
        <p class="property-card__address">{{ property.address }}</p>
      </div>
    </div>

    <div class="property-card__stats">
      <div class="property-card__stat">
        <span class="property-card__stat-value">{{ openCount }}</span>
        <span class="property-card__stat-label">{{ t('properties.card.openAppointments') }}</span>
      </div>
      <div class="property-card__stat">
        <span class="property-card__stat-value">{{ completedCount }}</span>
        <span class="property-card__stat-label">{{
          t('properties.card.completedAppointments')
        }}</span>
      </div>
    </div>

    <button
      v-if="nextAppointment"
      type="button"
      class="property-card__next-appointment"
      @click="$emit('open-appointment', nextAppointment.id)"
    >
      <span class="property-card__next-appointment-label">{{
        t('properties.card.nextAppointment', { title: nextAppointment.title })
      }}</span>
      <span class="property-card__next-appointment-value">{{ nextAppointmentLabel }}</span>
    </button>
    <p v-else class="property-card__no-next-appointment">
      {{ t('properties.card.noNextAppointment') }}
    </p>
  </div>
</template>

<style scoped src="@/styles/card-3d.css"></style>
<style scoped src="@/styles/properties/property-card.css"></style>
