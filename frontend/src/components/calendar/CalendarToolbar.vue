<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { VueCalView } from '@/types/vue-cal'

defineProps<{ rangeLabel: string }>()

const activeView = defineModel<VueCalView>('activeView', { required: true })

const emit = defineEmits<{ previous: []; next: []; today: [] }>()

const { t } = useI18n()

/** Views the toolbar lets the user switch between, in display order. */
const viewOptions: VueCalView[] = ['day', 'week', 'month', 'year']
</script>

<template>
  <div class="calendar-toolbar">
    <div class="calendar-toolbar__top">
      <div class="calendar-toolbar__range">
        <h2 class="calendar-toolbar__title">{{ rangeLabel }}</h2>
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
        {{ t(`calendar.toolbar.views.${view}`) }}
      </button>
    </div>
  </div>
</template>

<style scoped src="@/styles/calendar/calendar-toolbar.css"></style>
