<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppointmentsStore } from '@/stores/appointments'
import { useTodaysAppointments } from '@/composables/useTodaysAppointments'
import { useLocale } from '@/composables/useLocale'
import DailyAppointmentCard from '@/components/overview/DailyAppointmentCard.vue'
import AppointmentAiSuggestionBanner from '@/components/appointments/AppointmentAiSuggestionBanner.vue'

const { t } = useI18n()
const { currentLocale } = useLocale()
const appointmentsStore = useAppointmentsStore()
const { today, tomorrow, todaysAppointments, tomorrowsAppointments, allTodaysAppointmentsCompleted } =
  useTodaysAppointments()

/** Today's appointments still to do, hiding ones already marked as completed. */
const openAppointments = computed(() =>
  todaysAppointments.value.filter((appointment) => !appointment.completed),
)

/** Tomorrow's appointments still to do, shown as a preview once today is fully completed. */
const openTomorrowsAppointments = computed(() =>
  tomorrowsAppointments.value.filter((appointment) => !appointment.completed),
)

/** Formats a date as a localized "weekday, day month" heading. */
function formatHeadingDate(date: Date): string {
  return new Intl.DateTimeFormat(currentLocale.value, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(date)
}

const todayLabel = computed(() => formatHeadingDate(today.value))
const tomorrowDateLabel = computed(() =>
  new Intl.DateTimeFormat(currentLocale.value, { day: 'numeric', month: 'long' }).format(
    tomorrow.value,
  ),
)

const appointmentCountKey = computed(() =>
  openAppointments.value.length === 1
    ? 'overview.dailyList.appointmentCountSingular'
    : 'overview.dailyList.appointmentCountPlural',
)

onMounted(() => {
  appointmentsStore.fetchAppointments()
})
</script>

<template>
  <div class="daily-appointment-list card-3d">
    <template v-if="allTodaysAppointmentsCompleted">
      <div class="daily-appointment-list__header">
        <h2 class="daily-appointment-list__date">
          {{ t('overview.dailyList.tomorrowHeading', { date: tomorrowDateLabel }) }}
        </h2>
        <span class="daily-appointment-list__count">
          {{ t('overview.dailyList.allCompletedHint') }}
        </span>
      </div>

      <p v-if="openTomorrowsAppointments.length === 0" class="daily-appointment-list__empty">
        {{ t('overview.dailyList.emptyTomorrow') }}
      </p>
      <div v-else class="daily-appointment-list__items">
        <DailyAppointmentCard
          v-for="(appointment, index) in openTomorrowsAppointments"
          :key="appointment.id"
          :appointment="appointment"
          :position="index + 1"
          @open-appointment="appointmentsStore.openDetail"
        />
      </div>
    </template>
    <template v-else>
      <div class="daily-appointment-list__header">
        <h2 class="daily-appointment-list__date">{{ todayLabel }}</h2>
        <span class="daily-appointment-list__count">
          {{ t(appointmentCountKey, { count: openAppointments.length }) }} ·
          {{ t('overview.dailyList.clickHint') }}
        </span>
      </div>

      <p v-if="openAppointments.length === 0" class="daily-appointment-list__empty">
        {{ t('overview.dailyList.empty') }}
      </p>
      <div v-else class="daily-appointment-list__items">
        <DailyAppointmentCard
          v-for="(appointment, index) in openAppointments"
          :key="appointment.id"
          :appointment="appointment"
          :position="index + 1"
          @open-appointment="appointmentsStore.openDetail"
        />
      </div>
    </template>

    <AppointmentAiSuggestionBanner class="daily-appointment-list__ai-suggestion" />
  </div>
</template>

<style scoped src="@/styles/overview/daily-appointment-list.css"></style>
<style scoped src="@/styles/card-3d.css"></style>
