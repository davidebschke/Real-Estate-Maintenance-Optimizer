<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ViewPlaceholder from '@/components/ViewPlaceholder.vue'

const { t } = useI18n()
</script>

<template>
  <ViewPlaceholder :text="t('calendar.content')" />
</template>
