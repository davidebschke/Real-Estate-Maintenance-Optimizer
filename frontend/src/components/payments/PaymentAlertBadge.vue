<script setup lang="ts">
import { usePaymentStatus } from '@/composables/usePaymentStatus'
import type { PaymentAlert } from '@/types/payment'

defineProps<{ alert: PaymentAlert; compact?: boolean }>()

const { alertLabel, alertSeverity } = usePaymentStatus()
</script>

<template>
  <span
    v-if="alert !== 'NONE'"
    class="payment-alert-badge"
    :class="[`payment-severity--${alertSeverity(alert)}`, { 'payment-alert-badge--compact': compact }]"
    role="status"
    :aria-label="alertLabel(alert)"
    :title="alertLabel(alert)"
  >
    <i class="pi pi-exclamation-triangle" aria-hidden="true"></i>
    <span v-if="!compact" class="payment-alert-badge__label">{{ alertLabel(alert) }}</span>
  </span>
</template>

<style src="@/styles/payments/payment-severity.css"></style>
<style scoped src="@/styles/payments/payment-alert-badge.css"></style>
