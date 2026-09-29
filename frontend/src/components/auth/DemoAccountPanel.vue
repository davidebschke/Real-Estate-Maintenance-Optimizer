<script setup lang="ts">
import { computed, ref } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { toAuthFailureReason } from '@/services/authService'
import type { AuthFailureReason } from '@/types/auth'

/** Every fact about the demo account the user is told before creating one, with its icon. */
const FEATURES = [
  { key: 'properties', icon: 'pi-building' },
  { key: 'appointments', icon: 'pi-calendar' },
  { key: 'limits', icon: 'pi-plus-circle' },
  { key: 'session', icon: 'pi-clock' },
] as const

const emit = defineEmits<{ authenticated: [] }>()

const { t } = useI18n()
const authStore = useAuthStore()

const status = ref<'idle' | 'submitting' | 'success'>('idle')
const failureReason = ref<AuthFailureReason | null>(null)

const submitLabel = computed(() => {
  if (status.value === 'success') return t('auth.demo.success')
  if (status.value === 'submitting') return t('auth.demo.submitting')
  return t('auth.demo.submit')
})

/** Creates the demo account, showing the success state before notifying the parent, or the failure reason. */
async function createDemoAccount() {
  if (status.value !== 'idle') return

  status.value = 'submitting'
  failureReason.value = null
  try {
    await authStore.startDemoSession()
    status.value = 'success'
    emit('authenticated')
  } catch (error) {
    failureReason.value = toAuthFailureReason(error) === 'tooManyRequests' ? 'tooManyRequests' : 'generic'
    status.value = 'idle'
  }
}
</script>

<template>
  <div class="demo-account-panel">
    <h2 class="demo-account-panel__heading">{{ t('auth.demo.heading') }}</h2>
    <p class="demo-account-panel__description">{{ t('auth.demo.description') }}</p>

    <ul class="demo-account-panel__features">
      <li v-for="feature in FEATURES" :key="feature.key" class="demo-account-panel__feature">
        <i class="pi demo-account-panel__feature-icon" :class="feature.icon" aria-hidden="true"></i>
        <span>{{ t(`auth.demo.features.${feature.key}`) }}</span>
      </li>
    </ul>

    <p v-if="failureReason" class="demo-account-panel__error" role="alert">
      {{ t(`auth.demo.errors.${failureReason}`) }}
    </p>

    <Button
      type="button"
      class="demo-account-panel__submit"
      :class="{ 'demo-account-panel__submit--success': status === 'success' }"
      :label="submitLabel"
      :icon="status === 'success' ? 'pi pi-check' : 'pi pi-play'"
      :loading="status === 'submitting'"
      :disabled="status !== 'idle'"
      @click="createDemoAccount"
    />
  </div>
</template>

<style src="@/styles/auth/demo-account-panel.css"></style>
