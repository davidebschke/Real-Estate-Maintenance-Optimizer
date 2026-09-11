<script setup lang="ts">
import Dialog from 'primevue/dialog'
import { useI18n } from 'vue-i18n'

/** Controls visibility of the reusable "coming soon" notice dialog. */
const visible = defineModel<boolean>('visible', { required: true })

const { t } = useI18n()
</script>

<template>
  <Dialog v-model:visible="visible" modal :header="t('header.comingSoon.title')" :style="{ width: '24rem' }">
    <p>{{ t('header.comingSoon.message') }}</p>
  </Dialog>
</template>
