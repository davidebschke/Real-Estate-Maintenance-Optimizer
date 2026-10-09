<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOptimizationStore } from '@/stores/optimization'
import OptimizationLaunchCard from '@/components/optimization/OptimizationLaunchCard.vue'
import OptimizationProposalList from '@/components/optimization/OptimizationProposalList.vue'

const { t } = useI18n()
const store = useOptimizationStore()

onMounted(() => {
  store.fetchPendingProposals()
})
</script>

<template>
  <div class="optimization-view">
    <h1 class="optimization-view__heading">{{ t('optimization.heading') }}</h1>
    <p class="optimization-view__intro">{{ t('optimization.intro') }}</p>
    <div class="optimization-view__layout">
      <OptimizationLaunchCard class="optimization-view__launch" />
      <OptimizationProposalList class="optimization-view__proposals" />
    </div>
  </div>
</template>

<style scoped src="@/styles/optimization/optimization-view.css"></style>
