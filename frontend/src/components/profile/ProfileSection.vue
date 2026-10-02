<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { AccountFailureReason } from '@/types/auth'

defineProps<{
  headingId: string
  heading: string
  description: string
  failureReason: AccountFailureReason | null
  successMessage: string | null
}>()

const { t } = useI18n()
</script>

<template>
  <section class="profile-settings-dialog__section" :aria-labelledby="headingId">
    <h3 :id="headingId" class="profile-settings-dialog__heading">{{ heading }}</h3>
    <p class="profile-settings-dialog__description">{{ description }}</p>

    <slot />

    <p v-if="failureReason" class="profile-settings-dialog__field-error" role="alert">
      {{ t(`profile.errors.${failureReason}`) }}
    </p>
    <p v-if="successMessage" class="profile-settings-dialog__success" role="status">{{ successMessage }}</p>

    <slot name="actions" />
  </section>
</template>
