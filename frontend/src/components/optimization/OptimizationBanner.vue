<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useOptimizationStore } from '@/stores/optimization'
import { OPTIMIZATION_RULES } from '@/utils/optimizationRules'

const { t } = useI18n()
const store = useOptimizationStore()

const pendingCount = computed(() => store.pendingProposals.length)
const isUnavailable = computed(() => store.availability?.available === false)

const text = computed(() => {
  if (pendingCount.value > 0) return t('optimization.banner.pending', pendingCount.value)
  if (isUnavailable.value) return t('optimization.banner.unavailable')
  return t('optimization.banner.idle', OPTIMIZATION_RULES)
})

onMounted(() => {
  store.fetchPendingProposals()
  store.loadAvailability()
})
</script>

<template>
  <div class="optimization-banner" :class="{ 'optimization-banner--pending': pendingCount > 0 }">
    <i class="pi pi-sparkles optimization-banner__icon" aria-hidden="true"></i>
    <p class="optimization-banner__text">
      {{ text }}
    </p>
    <RouterLink :to="{ name: 'optimization' }" class="optimization-banner__action">
      {{ pendingCount > 0 ? t('optimization.banner.reviewButton') : t('optimization.banner.openButton') }}
    </RouterLink>
  </div>
</template>

<style scoped src="@/styles/optimization/optimization-banner.css"></style>
