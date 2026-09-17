<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'
import { formatDurationMinutes } from '@/utils/dateFormat'
import type { Appointment } from '@/types/appointment'

const props = defineProps<{
  appointment: Appointment
  position: number
}>()

defineEmits<{ 'open-appointment': [appointmentId: string] }>()

const { t } = useI18n()
const { currentLocale } = useLocale()

const startTimeLabel = computed(() =>
  new Intl.DateTimeFormat(currentLocale.value, { hour: '2-digit', minute: '2-digit' }).format(
    props.appointment.start,
  ),
)

const durationLabel = computed(() => {
  const durationMinutes = Math.round(
    (props.appointment.end.getTime() - props.appointment.start.getTime()) / 60000,
  )
  return formatDurationMinutes(durationMinutes, currentLocale.value)
})

const materialsLabel = computed(() => props.appointment.materials.join(', '))
</script>

<template>
  <button
    type="button"
    class="daily-appointment-card"
    :class="{ 'daily-appointment-card--completed': appointment.completed }"
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
        <span class="daily-appointment-card__travel">
          {{ t('overview.dailyList.travelSummary', { km: appointment.travelDistanceKm, duration: durationLabel }) }}
        </span>
      </div>
    </div>
  </button>
</template>

<style scoped src="@/styles/overview/daily-appointment-card.css"></style>
