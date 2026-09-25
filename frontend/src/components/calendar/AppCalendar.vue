<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VueCal from 'vue-cal'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import 'vue-cal/dist/vuecal.css'
import deLocale from 'vue-cal/dist/i18n/de.es.js'
import enLocale from 'vue-cal/dist/i18n/en.es.js'
import CalendarToolbar from '@/components/calendar/CalendarToolbar.vue'
import CalendarDayHeader from '@/components/calendar/CalendarDayHeader.vue'
import CalendarEventCard from '@/components/calendar/CalendarEventCard.vue'
import { useLocale } from '@/composables/useLocale'
import { useCalendarNavigation } from '@/composables/useCalendarNavigation'
import { useCalendarAppointments } from '@/composables/useCalendarAppointments'
import { useAppointmentDragAndDrop } from '@/composables/useAppointmentDragAndDrop'
import { useAppointmentsStore } from '@/stores/appointments'
import type { VueCalEventClickEvent, VueCalSlotEvent, VueCalWeekdayHeading } from '@/types/vue-cal'

const { t } = useI18n()
const { currentLocale } = useLocale()
const toast = useToast()
const {
  vueCalRef,
  activeView,
  visibleRange,
  rangeLabel,
  handleViewChange,
  goToPrevious,
  goToNext,
  goToToday,
} = useCalendarNavigation()
const { events, getDaySummary } = useCalendarAppointments()
const appointmentsStore = useAppointmentsStore()

/** Milliseconds the "this appointment can't be dragged" toast stays visible before it dismisses itself. */
const TOAST_LIFE_MS = 3000

let isLockedDragToastVisible = false

/** Shows the "this appointment can't be dragged" toast, ignoring a repeat attempt while one is already visible so they don't stack. */
function showLockedDragToast() {
  if (isLockedDragToastVisible) return
  isLockedDragToastVisible = true
  toast.add({ severity: 'warn', detail: t('calendar.dragLocked'), life: TOAST_LIFE_MS })
  setTimeout(() => {
    isLockedDragToastVisible = false
  }, TOAST_LIFE_MS)
}

const calendarGridRef = ref<HTMLElement | null>(null)
const {
  preview: dragPreview,
  handlePointerDown,
  wasLastInteractionADrag,
} = useAppointmentDragAndDrop(
  calendarGridRef,
  activeView,
  visibleRange,
  async (appointmentId, newStart, durationMinutes) => {
    await appointmentsStore.moveAppointment(appointmentId, { start: newStart, durationMinutes })
  },
  showLockedDragToast,
)

/** vue-cal's ISO weekday number for Sunday, hidden from every calendar view since the company does not schedule appointments then. */
const SUNDAY_ISO_WEEKDAY = 7

/** Locale bundle for vue-cal's own built-in labels (weekday/month names), matching the active app locale, with its default "no event" label overridden to this app's "Termin" terminology. */
const vueCalLocale = computed(() => ({
  ...(currentLocale.value === 'de' ? deLocale : enLocale),
  noEvent: t('calendar.noAppointments'),
}))

/** Opens the detail view for the clicked appointment, unless that same press just dragged it. */
function handleEventClick(event: VueCalEventClickEvent) {
  if (wasLastInteractionADrag()) return
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

    <div
      ref="calendarGridRef"
      class="app-calendar__drag-container"
      @pointerdown="handlePointerDown"
    >
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
            :appointment-id="event.appointmentId"
            :title="event.title"
            :property-name="event.content"
            :start="event.start"
            :end="event.end"
          />
        </template>
      </VueCal>

      <div
        v-if="dragPreview.isDragging"
        class="app-calendar__drag-preview"
        :class="{ 'app-calendar__drag-preview--invalid': !dragPreview.isValidDrop }"
        :style="{ top: `${dragPreview.pointerY}px`, left: `${dragPreview.pointerX}px` }"
      >
        {{ dragPreview.label }}
      </div>
    </div>
  </div>
</template>

<style scoped src="@/styles/calendar/app-calendar.css"></style>
