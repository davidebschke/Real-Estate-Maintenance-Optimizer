<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  appointmentId: string
  title: string
  propertyName: string
  start: Date
  end: Date
}>()

const { locale } = useI18n()

const timeRangeLabel = computed(() => {
  const timeFormatter = new Intl.DateTimeFormat(locale.value, {
    hour: '2-digit',
    minute: '2-digit',
  })
  return `${timeFormatter.format(props.start)}–${timeFormatter.format(props.end)}`
})

const fullLabel = computed(() => `${props.title} · ${props.propertyName} · ${timeRangeLabel.value}`)
</script>

<template>
  <div class="calendar-event-card" :title="fullLabel" :data-appointment-id="appointmentId">
    <span class="calendar-event-card__title">{{ title }}</span>
    <span class="calendar-event-card__meta">{{ propertyName }} · {{ timeRangeLabel }}</span>
  </div>
</template>

<style scoped src="@/styles/calendar/calendar-event-card.css"></style>
