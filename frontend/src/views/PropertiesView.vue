<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import PropertyList from '@/components/properties/PropertyList.vue'

const { t } = useI18n()
</script>

<template>
  <div class="properties-view">
    <h1 class="properties-view__heading">{{ t('properties.list.heading') }}</h1>
    <PropertyList />
  </div>
</template>

<style scoped src="@/styles/properties/properties-view.css"></style>
