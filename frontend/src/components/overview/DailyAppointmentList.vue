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
const { todaysAppointments } = useTodaysAppointments()

/** Today's appointments still to do, hiding ones already marked as completed. */
const openAppointments = computed(() =>
  todaysAppointments.value.filter((appointment) => !appointment.completed),
)

const todayLabel = computed(() =>
  new Intl.DateTimeFormat(currentLocale.value, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date()),
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

    <AppointmentAiSuggestionBanner class="daily-appointment-list__ai-suggestion" />
  </div>
</template>

<style scoped src="@/styles/overview/daily-appointment-list.css"></style>
<style scoped src="@/styles/card-3d.css"></style>
