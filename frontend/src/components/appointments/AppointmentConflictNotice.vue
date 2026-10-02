<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSuggestedSlotLabel } from '@/composables/useSuggestedSlotLabel'
import type { AppointmentConflict } from '@/services/appointmentService'

const props = defineProps<{ conflict: AppointmentConflict }>()
const emit = defineEmits<{ accept: [] }>()

const { t } = useI18n()
const { formatSuggestedSlot } = useSuggestedSlotLabel()

const suggestedSlotLine = computed(() => formatSuggestedSlot(props.conflict))
</script>

<template>
  <div class="appointment-conflict-notice">
    <p class="appointment-conflict-notice__text">{{ conflict.message }}</p>
    <p class="appointment-conflict-notice__text">
      {{ t('appointments.conflict.suggestion', { slot: suggestedSlotLine }) }}
    </p>
    <button type="button" class="appointment-conflict-notice__accept" @click="emit('accept')">
      {{ t('appointments.conflict.acceptSuggestion') }}
    </button>
  </div>
</template>

<style scoped src="@/styles/appointments/appointment-conflict-notice.css"></style>
