<script setup lang="ts">
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <Button
    class="appointment-button"
    :label="t('header.appointment.createButton')"
    size="small"
    disabled
  />
</template>

<style scoped src="@/styles/layout/appointment-button.css"></style>
