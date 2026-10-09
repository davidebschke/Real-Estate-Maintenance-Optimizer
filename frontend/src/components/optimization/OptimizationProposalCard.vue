<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'
import { formatLocalizedDateTime, formatLocalizedTime } from '@/utils/dateFormat'
import { formatKilometers, formatTravelDuration } from '@/utils/travelFormat'
import type { OptimizationProposal } from '@/types/optimization'

const props = defineProps<{ proposal: OptimizationProposal; deciding: boolean }>()

const emit = defineEmits<{
  accept: [proposalId: string]
  reject: [proposalId: string]
}>()

const { t } = useI18n()
const { currentLocale } = useLocale()

/** Formats a schedule as its localized start date and time followed by its end time. */
function formatSchedule(start: Date, end: Date): string {
  return `${formatLocalizedDateTime(start, currentLocale.value)}–${formatLocalizedTime(end, currentLocale.value)}`
}

const originalSchedule = computed(() => formatSchedule(props.proposal.originalStart, props.proposal.originalEnd))
const proposedSchedule = computed(() => formatSchedule(props.proposal.proposedStart, props.proposal.proposedEnd))
const savedText = computed(() =>
  t('optimization.proposals.savedValue', {
    distance: formatKilometers(props.proposal.savedDistanceMeters, currentLocale.value),
    duration: formatTravelDuration(props.proposal.savedDurationSeconds, currentLocale.value),
  }),
)
</script>

<template>
  <article class="optimization-proposal-card">
    <header class="optimization-proposal-card__header">
      <h3 class="optimization-proposal-card__title">{{ proposal.appointmentTitle }}</h3>
      <span v-if="proposal.recurring" class="optimization-proposal-card__badge">
        <i class="pi pi-replay" aria-hidden="true"></i>
        {{ t('optimization.proposals.recurring') }}
      </span>
      <p class="optimization-proposal-card__property">{{ proposal.propertyName }}</p>
    </header>

    <dl class="optimization-proposal-card__schedule">
      <div class="optimization-proposal-card__slot">
        <dt>{{ t('optimization.proposals.from') }}</dt>
        <dd class="optimization-proposal-card__original">{{ originalSchedule }}</dd>
      </div>
      <i class="pi pi-arrow-right optimization-proposal-card__arrow" aria-hidden="true"></i>
      <div class="optimization-proposal-card__slot">
        <dt>{{ t('optimization.proposals.to') }}</dt>
        <dd class="optimization-proposal-card__proposed">{{ proposedSchedule }}</dd>
      </div>
    </dl>

    <p class="optimization-proposal-card__saving">
      <i class="pi pi-bolt" aria-hidden="true"></i>
      <span>{{ t('optimization.proposals.saved') }}: {{ savedText }}</span>
    </p>

    <p v-if="proposal.reason" class="optimization-proposal-card__reason">
      <span class="optimization-proposal-card__reason-label">{{ t('optimization.proposals.reason') }}:</span>
      {{ proposal.reason }}
    </p>

    <div class="optimization-proposal-card__actions">
      <Button
        class="optimization-proposal-card__accept"
        icon="pi pi-check"
        :label="t('optimization.proposals.accept')"
        :loading="deciding"
        :disabled="deciding"
        @click="emit('accept', proposal.id)"
      />
      <Button
        class="optimization-proposal-card__reject"
        icon="pi pi-times"
        severity="secondary"
        outlined
        :label="t('optimization.proposals.reject')"
        :disabled="deciding"
        @click="emit('reject', proposal.id)"
      />
    </div>
  </article>
</template>

<style scoped src="@/styles/optimization/optimization-proposal-card.css"></style>
