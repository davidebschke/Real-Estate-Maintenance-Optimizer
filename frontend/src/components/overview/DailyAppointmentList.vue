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

const todayLabel = computed(() =>
  new Intl.DateTimeFormat(currentLocale.value, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date()),
)

const appointmentCountKey = computed(() =>
  todaysAppointments.value.length === 1
    ? 'overview.dailyList.appointmentCountSingular'
    : 'overview.dailyList.appointmentCountPlural',
)

onMounted(() => {
  appointmentsStore.fetchAppointments()
})
</script>

<template>
  <div class="daily-appointment-list">
    <div class="daily-appointment-list__header">
      <h2 class="daily-appointment-list__date">{{ todayLabel }}</h2>
      <span class="daily-appointment-list__count">
        {{ t(appointmentCountKey, { count: todaysAppointments.length }) }} ·
        {{ t('overview.dailyList.clickHint') }}
      </span>
    </div>

    <p v-if="todaysAppointments.length === 0" class="daily-appointment-list__empty">
      {{ t('overview.dailyList.empty') }}
    </p>
    <div v-else class="daily-appointment-list__items">
      <DailyAppointmentCard
        v-for="(appointment, index) in todaysAppointments"
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
