<script setup lang="ts">
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <div class="appointment-ai-suggestion-banner">
    <p class="appointment-ai-suggestion-banner__text">
      {{ t('appointments.form.aiSuggestion.text') }}
    </p>
    <Button
      class="appointment-ai-suggestion-banner__action"
      :label="t('appointments.form.aiSuggestion.applyButton')"
      size="small"
    />
  </div>
</template>

<style scoped src="@/styles/appointments/appointment-ai-suggestion-banner.css"></style>
