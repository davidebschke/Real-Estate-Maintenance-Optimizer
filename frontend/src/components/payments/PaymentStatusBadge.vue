<script setup lang="ts">
import { computed } from 'vue'
import { usePaymentStatus } from '@/composables/usePaymentStatus'
import type { MonthPaymentStatus } from '@/types/payment'

const props = defineProps<{ status: MonthPaymentStatus; detail?: string }>()

const { statusLabel, statusIcon, statusSeverity } = usePaymentStatus()

const label = computed(() =>
  props.detail ? `${statusLabel(props.status)}: ${props.detail}` : statusLabel(props.status),
)
</script>

<template>
  <span
    class="payment-status-badge"
    :class="`payment-severity--${statusSeverity(status)}`"
    role="img"
    :aria-label="label"
    :title="label"
  >
    <i class="pi" :class="statusIcon(status)" aria-hidden="true"></i>
  </span>
</template>

<style src="@/styles/payments/payment-severity.css"></style>
<style scoped src="@/styles/payments/payment-status-badge.css"></style>
