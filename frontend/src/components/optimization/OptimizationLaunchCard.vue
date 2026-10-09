<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { useOptimizationStore } from '@/stores/optimization'
import { useDemoQuota } from '@/composables/useDemoQuota'
import { OPTIMIZATION_RULES } from '@/utils/optimizationRules'
import DemoQuotaHint from '@/components/auth/DemoQuotaHint.vue'

const { t } = useI18n()
const store = useOptimizationStore()
const { isExhausted } = useDemoQuota('aiOptimizations')

const ruleKeys = ['window', 'locked', 'recurring', 'travel'] as const

const lastRunSummary = computed(() => {
  const run = store.lastRun
  if (!run) return null
  return t('optimization.launch.lastRun', {
    analyzed: run.analyzedAppointmentCount,
    candidates: run.candidateCount,
    proposals: run.proposals.length,
  })
})
</script>

<template>
  <section class="optimization-launch-card" :aria-label="t('optimization.launch.title')">
    <h2 class="optimization-launch-card__title">
      <i class="pi pi-sparkles" aria-hidden="true"></i>
      {{ t('optimization.rules.title') }}
    </h2>
    <ul class="optimization-launch-card__rules">
      <li v-for="ruleKey in ruleKeys" :key="ruleKey" class="optimization-launch-card__rule">
        <i class="pi pi-check-circle" aria-hidden="true"></i>
        <span>{{ t(`optimization.rules.${ruleKey}`, OPTIMIZATION_RULES) }}</span>
      </li>
    </ul>

    <DemoQuotaHint resource="aiOptimizations" />

    <Button
      class="optimization-launch-card__start"
      icon="pi pi-play"
      :label="store.isRunning ? t('optimization.launch.running') : t('optimization.launch.button')"
      :loading="store.isRunning"
      :disabled="store.isRunning || isExhausted"
      @click="store.startRun"
    />

    <p v-if="store.hasRunError" class="optimization-launch-card__error" role="alert">
      {{ store.runErrorMessage ?? t('optimization.launch.error') }}
    </p>
    <p v-if="lastRunSummary" class="optimization-launch-card__summary" role="status">
      {{ lastRunSummary }}
      <template v-if="store.lastRun?.proposals.length === 0"> {{ t('optimization.launch.noProposals') }}</template>
    </p>
  </section>
</template>

<style scoped src="@/styles/optimization/optimization-launch-card.css"></style>
