<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'
import { useAppointmentsStore } from '@/stores/appointments'
import { isSameDay } from '@/utils/dateFormat'
import type { Appointment } from '@/types/appointment'

const props = defineProps<{
  visibleStart: Date
  visibleEnd: Date
  appointments: Appointment[]
}>()

const { t } = useI18n()
const { currentLocale } = useLocale()
const appointmentsStore = useAppointmentsStore()

/** One calendar day within the visible range, with the appointments scheduled on it, sorted by start time. */
interface DayGroup {
  date: Date
  appointments: Appointment[]
}

const dayGroups = computed<DayGroup[]>(() =>
  buildDayGroups(props.appointments, props.visibleStart, props.visibleEnd),
)

/** Formats a date as a localized "weekday, day month" section heading. */
function formatDayHeading(date: Date): string {
  return new Intl.DateTimeFormat(currentLocale.value, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(date)
}

/** Formats the start time of day as localized hours and minutes. */
function formatTime(date: Date): string {
  return new Intl.DateTimeFormat(currentLocale.value, { hour: '2-digit', minute: '2-digit' }).format(
    date,
  )
}

function appointmentCountKey(count: number): string {
  return count === 1
    ? 'calendar.dayHeader.appointmentCountSingular'
    : 'calendar.dayHeader.appointmentCountPlural'
}

/** Groups the given appointments by calendar day across the visible range, in ascending day and start-time order, omitting empty days. */
function buildDayGroups(appointments: Appointment[], rangeStart: Date, rangeEnd: Date): DayGroup[] {
  const groups: DayGroup[] = []
  const cursor = new Date(rangeStart.getFullYear(), rangeStart.getMonth(), rangeStart.getDate())
  const end = new Date(rangeEnd.getFullYear(), rangeEnd.getMonth(), rangeEnd.getDate())

  while (cursor <= end) {
    const date = new Date(cursor)
    const dayAppointments = appointments
      .filter((appointment) => isSameDay(appointment.start, date))
      .sort((a, b) => a.start.getTime() - b.start.getTime())
    if (dayAppointments.length > 0) groups.push({ date, appointments: dayAppointments })
    cursor.setDate(cursor.getDate() + 1)
  }

  return groups
}
</script>

<template>
  <div class="calendar-list-view">
    <p v-if="dayGroups.length === 0" class="calendar-list-view__empty">
      {{ t('calendar.list.empty') }}
    </p>

    <section v-for="group in dayGroups" :key="group.date.toISOString()" class="calendar-list-view__day">
      <div class="calendar-list-view__day-header">
        <h3 class="calendar-list-view__day-heading">{{ formatDayHeading(group.date) }}</h3>
        <span class="calendar-list-view__day-count">
          {{ t(appointmentCountKey(group.appointments.length), { count: group.appointments.length }) }}
        </span>
      </div>

      <ul class="calendar-list-view__items">
        <li v-for="appointment in group.appointments" :key="appointment.id">
          <button
            type="button"
            class="calendar-list-view__item"
            :class="{ 'calendar-list-view__item--completed': appointment.completed }"
            @click="appointmentsStore.openDetail(appointment.id)"
          >
            <span class="calendar-list-view__time">{{ formatTime(appointment.start) }}</span>
            <span class="calendar-list-view__title">{{ appointment.title }}</span>
            <span class="calendar-list-view__property">{{ appointment.propertyName }}</span>
          </button>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped src="@/styles/calendar/calendar-list-view.css"></style>
