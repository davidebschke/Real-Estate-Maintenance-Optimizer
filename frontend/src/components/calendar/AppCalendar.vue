<script setup lang="ts">
import { computed, onMounted } from 'vue'
import VueCal from 'vue-cal'
import { useI18n } from 'vue-i18n'
import 'vue-cal/dist/vuecal.css'
import deLocale from 'vue-cal/dist/i18n/de.es.js'
import enLocale from 'vue-cal/dist/i18n/en.es.js'
import CalendarToolbar from '@/components/calendar/CalendarToolbar.vue'
import CalendarDayHeader from '@/components/calendar/CalendarDayHeader.vue'
import CalendarEventCard from '@/components/calendar/CalendarEventCard.vue'
import { useLocale } from '@/composables/useLocale'
import { useCalendarNavigation } from '@/composables/useCalendarNavigation'
import { useCalendarAppointments } from '@/composables/useCalendarAppointments'
import { useAppointmentsStore } from '@/stores/appointments'
import type { VueCalEventClickEvent, VueCalSlotEvent, VueCalWeekdayHeading } from '@/types/vue-cal'

const { t } = useI18n()
const { currentLocale } = useLocale()
const { vueCalRef, activeView, rangeLabel, handleViewChange, goToPrevious, goToNext, goToToday } =
  useCalendarNavigation()
const { events, getDaySummary } = useCalendarAppointments()
const appointmentsStore = useAppointmentsStore()

/** vue-cal's ISO weekday number for Sunday, hidden from every calendar view since the company does not schedule appointments then. */
const SUNDAY_ISO_WEEKDAY = 7

/** Locale bundle for vue-cal's own built-in labels (weekday/month names), matching the active app locale, with its default "no event" label overridden to this app's "Termin" terminology. */
const vueCalLocale = computed(() => ({
  ...(currentLocale.value === 'de' ? deLocale : enLocale),
  noEvent: t('calendar.noAppointments'),
}))

/** Opens the detail view for the clicked appointment. */
function handleEventClick(event: VueCalEventClickEvent) {
  appointmentsStore.openDetail(event.appointmentId)
}

onMounted(() => {
  appointmentsStore.fetchAppointments()
})
</script>

<template>
  <div class="app-calendar">
    <CalendarToolbar
      v-model:active-view="activeView"
      :range-label="rangeLabel"
      @previous="goToPrevious"
      @next="goToNext"
      @today="goToToday"
    />

    <VueCal
      ref="vueCalRef"
      v-model:active-view="activeView"
      class="app-calendar__grid"
      :disable-views="['years']"
      :hide-weekdays="[SUNDAY_ISO_WEEKDAY]"
      :locale="vueCalLocale"
      :events="events"
      :time-from="420"
      :time-to="1140"
      :time-step="60"
      :hide-title-bar="true"
      :hide-view-selector="true"
      :on-event-click="handleEventClick"
      @view-change="handleViewChange"
    >
      <template #weekday-heading="{ heading }: { heading: VueCalWeekdayHeading }">
        <CalendarDayHeader
          :date="heading.date"
          :label="heading.label"
          :summary="heading.date ? getDaySummary(heading.date) : null"
        />
      </template>

      <template #event="{ event }: { event: VueCalSlotEvent }">
        <CalendarEventCard
          :title="event.title"
          :property-name="event.content"
          :start="event.start"
          :end="event.end"
        />
      </template>
    </VueCal>
  </div>
</template>

<style scoped src="@/styles/calendar/app-calendar.css"></style>
