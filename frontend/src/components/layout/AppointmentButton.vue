<script setup lang="ts">
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { useAppointmentsStore } from '@/stores/appointments'

const { t } = useI18n()
const appointmentsStore = useAppointmentsStore()
</script>

<template>
  <Button
    class="appointment-button"
    :label="t('header.appointment.newDateButton')"
    size="small"
    @click="appointmentsStore.openCreateDialog()"
  />
</template>

<style scoped src="@/styles/layout/appointment-button.css"></style>
