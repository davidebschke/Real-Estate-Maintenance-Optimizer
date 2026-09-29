<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useDemoQuota } from '@/composables/useDemoQuota'
import { useAccountPresentation } from '@/composables/useAccountPresentation'

const { t } = useI18n()
const authStore = useAuthStore()
const { sessionEndTime } = useAccountPresentation()
const { remaining: remainingProperties } = useDemoQuota('properties')
const { remaining: remainingAppointments } = useDemoQuota('appointments')

const remainingText = computed(() =>
  t('auth.banner.remaining', {
    properties: t('auth.quota.properties', remainingProperties.value ?? 0),
    appointments: t('auth.quota.appointments', remainingAppointments.value ?? 0),
  }),
)
</script>

<template>
  <div v-if="authStore.isDemoAccount" class="demo-account-banner" role="status">
    <i class="pi pi-info-circle demo-account-banner__icon" aria-hidden="true"></i>
    <p class="demo-account-banner__text">
      <strong class="demo-account-banner__title">{{ t('auth.banner.title') }}</strong>
      <span>{{ t('auth.banner.message', { time: sessionEndTime }) }}</span>
      <span>{{ remainingText }}</span>
    </p>
  </div>
</template>

<style scoped src="@/styles/auth/demo-account-banner.css"></style>
