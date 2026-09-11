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

<style scoped>
.appointment-button.p-button {
  background-color: #ffffff;
  border-color: #ffffff;
  color: #000000;
  font-size: 1.05rem;
  opacity: 1;
  padding: 0.45rem 0.75rem;
}
</style>
