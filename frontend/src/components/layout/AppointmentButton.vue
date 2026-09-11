<script setup lang="ts">
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <Button :label="t('header.appointment.createButton')" severity="secondary" size="small" disabled />
</template>
