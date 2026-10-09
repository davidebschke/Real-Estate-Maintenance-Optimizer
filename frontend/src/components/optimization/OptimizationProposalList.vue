<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useOptimizationStore } from '@/stores/optimization'
import OptimizationProposalCard from '@/components/optimization/OptimizationProposalCard.vue'

const { t } = useI18n()
const store = useOptimizationStore()
</script>

<template>
  <section class="optimization-proposal-list" :aria-label="t('optimization.proposals.heading')">
    <h2 class="optimization-proposal-list__heading">
      {{ t('optimization.proposals.heading') }}
      <span class="optimization-proposal-list__count">{{ store.pendingProposals.length }}</span>
    </h2>

    <p v-if="store.hasLoadError" class="optimization-proposal-list__error" role="alert">
      {{ t('optimization.proposals.loadError') }}
    </p>
    <p v-if="store.hasDecisionError" class="optimization-proposal-list__error" role="alert">
      {{ store.decisionErrorMessage ?? t('optimization.proposals.decisionError') }}
    </p>

    <p v-if="store.pendingProposals.length === 0" class="optimization-proposal-list__empty">
      {{ t('optimization.proposals.empty') }}
    </p>
    <div v-else class="optimization-proposal-list__items">
      <OptimizationProposalCard
        v-for="proposal in store.pendingProposals"
        :key="proposal.id"
        :proposal="proposal"
        :deciding="store.isDeciding(proposal.id)"
        @accept="store.acceptProposal"
        @reject="store.rejectProposal"
      />
    </div>
  </section>
</template>

<style scoped src="@/styles/optimization/optimization-proposal-list.css"></style>
