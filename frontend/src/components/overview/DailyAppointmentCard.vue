<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'
import { useCardTilt } from '@/composables/useCardTilt'
import { formatDurationMinutes } from '@/utils/dateFormat'
import { formatKilometers, formatTravelDuration } from '@/utils/travelFormat'
import { getDurationMinutes } from '@/utils/appointmentSchedulingOptions'
import type { Appointment } from '@/types/appointment'
import type { RouteLeg } from '@/types/route'

const props = defineProps<{
  appointment: Appointment
  position: number
  travelLeg?: RouteLeg | null
}>()

defineEmits<{ 'open-appointment': [appointmentId: string] }>()

const { t } = useI18n()
const { currentLocale } = useLocale()
const { tiltStyle, onPointerMove, onPointerLeave } = useCardTilt()

const startTimeLabel = computed(() =>
  new Intl.DateTimeFormat(currentLocale.value, { hour: '2-digit', minute: '2-digit' }).format(
    props.appointment.start,
  ),
)

const durationLabel = computed(() => {
  const durationMinutes = getDurationMinutes(props.appointment.start, props.appointment.end)
  return formatDurationMinutes(durationMinutes, currentLocale.value)
})

const travelLabel = computed(() =>
  props.travelLeg
    ? t('overview.dailyList.travelSummary', {
        km: formatKilometers(props.travelLeg.distanceMeters, currentLocale.value),
        duration: formatTravelDuration(props.travelLeg.durationSeconds, currentLocale.value),
      })
    : null,
)

const materialsLabel = computed(() => props.appointment.materials.join(', '))
</script>

<template>
  <button
    type="button"
    class="daily-appointment-card card-3d card-3d--interactive"
    :class="{ 'daily-appointment-card--completed': appointment.completed }"
    :style="tiltStyle"
    @pointermove="onPointerMove"
    @pointerleave="onPointerLeave"
    @click="$emit('open-appointment', appointment.id)"
  >
    <span class="daily-appointment-card__position">{{ position }}</span>
    <div class="daily-appointment-card__body">
      <div class="daily-appointment-card__headline">
        <span class="daily-appointment-card__time">{{ startTimeLabel }}</span>
        <span class="daily-appointment-card__title">{{ appointment.title }}</span>
      </div>
      <p class="daily-appointment-card__property">
        {{ appointment.propertyName }} · {{ appointment.propertyAddress }}
      </p>
      <div class="daily-appointment-card__meta">
        <span v-if="materialsLabel" class="daily-appointment-card__materials">{{
          materialsLabel
        }}</span>
        <span v-if="travelLabel" class="daily-appointment-card__travel">{{ travelLabel }}</span>
        <span class="daily-appointment-card__duration">
          {{ t('overview.dailyList.durationSummary', { duration: durationLabel }) }}
        </span>
      </div>
    </div>
  </button>
</template>

<style scoped src="@/styles/overview/daily-appointment-card.css"></style>
<style scoped src="@/styles/card-3d.css"></style>
