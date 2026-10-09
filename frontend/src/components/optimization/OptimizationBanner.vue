<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useOptimizationStore } from '@/stores/optimization'

const { t } = useI18n()
const store = useOptimizationStore()

const pendingCount = computed(() => store.pendingProposals.length)

onMounted(() => {
  store.fetchPendingProposals()
})
</script>

<template>
  <div class="optimization-banner" :class="{ 'optimization-banner--pending': pendingCount > 0 }">
    <i class="pi pi-sparkles optimization-banner__icon" aria-hidden="true"></i>
    <p class="optimization-banner__text">
      {{ pendingCount > 0 ? t('optimization.banner.pending', pendingCount) : t('optimization.banner.idle') }}
    </p>
    <RouterLink :to="{ name: 'optimization' }" class="optimization-banner__action">
      {{ pendingCount > 0 ? t('optimization.banner.reviewButton') : t('optimization.banner.openButton') }}
    </RouterLink>
  </div>
</template>

<style scoped src="@/styles/optimization/optimization-banner.css"></style>
