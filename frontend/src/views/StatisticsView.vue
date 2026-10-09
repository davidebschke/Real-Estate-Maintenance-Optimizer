<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import StatisticsMenu from '@/components/statistics/StatisticsMenu.vue'

const { t } = useI18n()
</script>

<template>
  <div class="statistics-view">
    <h1 class="statistics-view__heading">{{ t('statistics.heading') }}</h1>
    <StatisticsMenu />
  </div>
</template>

<style scoped src="@/styles/statistics/statistics-view.css"></style>
