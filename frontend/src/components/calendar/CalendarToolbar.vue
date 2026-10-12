<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { AppCalendarView } from '@/types/vue-cal'

defineProps<{ rangeLabel: string }>()

const activeView = defineModel<AppCalendarView>('activeView', { required: true })

const emit = defineEmits<{ previous: []; next: []; today: [] }>()

const { t } = useI18n()

/** Views the toolbar offers, i.e. every calendar view except vue-cal's disabled multi-year view. */
type ToolbarView = Exclude<AppCalendarView, 'years'>

/** Views the toolbar lets the user switch between, in display order. */
const viewOptions: ToolbarView[] = ['day', 'week', 'month', 'year', 'list']

/** PrimeIcons class shown in front of the label of each view button. */
const viewIcons: Record<ToolbarView, string> = {
  day: 'pi-sun',
  week: 'pi-table',
  month: 'pi-calendar',
  year: 'pi-th-large',
  list: 'pi-list',
}
</script>

<template>
  <div class="calendar-toolbar">
    <div class="calendar-toolbar__top">
      <div class="calendar-toolbar__range">
        <h2 class="calendar-toolbar__title">{{ rangeLabel }}</h2>
        <template v-if="activeView !== 'list'">
          <button
            type="button"
            class="calendar-toolbar__nav-button"
            :aria-label="t('calendar.toolbar.previous')"
            @click="emit('previous')"
          >
            <i class="pi pi-chevron-left" aria-hidden="true"></i>
          </button>
          <button type="button" class="calendar-toolbar__today-button" @click="emit('today')">
            {{ t('calendar.toolbar.today') }}
          </button>
          <button
            type="button"
            class="calendar-toolbar__nav-button"
            :aria-label="t('calendar.toolbar.next')"
            @click="emit('next')"
          >
            <i class="pi pi-chevron-right" aria-hidden="true"></i>
          </button>
        </template>
      </div>

      <p class="calendar-toolbar__hint">{{ t('calendar.toolbar.hint') }}</p>
    </div>

    <div
      class="calendar-toolbar__views"
      role="group"
      :aria-label="t('calendar.toolbar.viewSwitcher')"
    >
      <button
        v-for="view in viewOptions"
        :key="view"
        type="button"
        class="calendar-toolbar__view-button"
        :class="{ 'calendar-toolbar__view-button--active': activeView === view }"
        @click="activeView = view"
      >
        <i class="pi calendar-toolbar__view-icon" :class="viewIcons[view]" aria-hidden="true"></i>
        <span class="calendar-toolbar__view-label">{{ t(`calendar.toolbar.views.${view}`) }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped src="@/styles/calendar/calendar-toolbar.css"></style>
