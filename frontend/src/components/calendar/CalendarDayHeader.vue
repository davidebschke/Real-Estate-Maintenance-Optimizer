<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { DayAppointmentSummary } from '@/composables/useCalendarAppointments'
import { formatLocalizedDayNumber } from '@/utils/dateFormat'

const props = defineProps<{
  date?: Date
  label: string
  summary: DayAppointmentSummary | null
}>()

const { t, locale } = useI18n()

/** Day-of-month number, only shown for the day and week views where a specific date is known. */
const dayNumber = computed(() =>
  props.date ? formatLocalizedDayNumber(props.date, locale.value) : null,
)

/** Translation key for the appointment count, chosen so singular and plural read correctly. */
const appointmentCountKey = computed(() =>
  props.summary?.appointmentCount === 1
    ? 'calendar.dayHeader.appointmentCountSingular'
    : 'calendar.dayHeader.appointmentCountPlural',
)
</script>

<template>
  <div class="calendar-day-header">
    <span class="calendar-day-header__weekday">{{
      dayNumber ? `${label} ${dayNumber}` : label
    }}</span>
    <span v-if="summary" class="calendar-day-header__summary">
      {{ t(appointmentCountKey, { count: summary.appointmentCount }) }} ·
      {{ t('calendar.dayHeader.travelDistance', { km: summary.travelDistanceKm }) }}
    </span>
  </div>
</template>

<style scoped src="@/styles/calendar/calendar-day-header.css"></style>
