<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import PaymentAlertBadge from '@/components/payments/PaymentAlertBadge.vue'
import PaymentStatusBadge from '@/components/payments/PaymentStatusBadge.vue'
import { useApartmentFormatting } from '@/composables/useApartmentFormatting'
import { useLocale } from '@/composables/useLocale'
import { MONTH_STATUSES, usePaymentStatus } from '@/composables/usePaymentStatus'
import { formatShortMonth } from '@/utils/paymentPeriods'
import type { MonthPayment, TenantPaymentYear } from '@/types/payment'

/** Earliest year the table can be navigated to. */
const MIN_YEAR = 2000
const MONTHS = Array.from({ length: 12 }, (_, index) => index + 1)

const props = defineProps<{ year: number; tenants: TenantPaymentYear[]; isLoading: boolean }>()

const emit = defineEmits<{ 'change-year': [year: number] }>()

const { t } = useI18n()
const { currentLocale } = useLocale()
const { formatFloor, formatMoney } = useApartmentFormatting()
const { statusLabel } = usePaymentStatus()

const maxYear = computed(() => new Date().getFullYear() + 1)
const monthLabels = computed(() => MONTHS.map((month) => formatShortMonth(month, currentLocale.value)))

/** Returns the full name of the tenant. */
function fullName(tenant: TenantPaymentYear): string {
  return `${tenant.firstName} ${tenant.lastName}`
}

/** Returns the amount paid in a month as money, or undefined if nothing was paid. */
function paidDetail(month: MonthPayment): string | undefined {
  return month.paidAmount > 0 ? formatMoney(month.paidAmount) : undefined
}

/** Asks for the table of another year. */
function changeYear(delta: number) {
  emit('change-year', props.year + delta)
}
</script>

<template>
  <section class="tenant-payment-table" :aria-busy="isLoading">
    <div class="tenant-payment-table__nav">
      <Button
        class="tenant-payment-table__previous"
        icon="pi pi-chevron-left"
        severity="secondary"
        text
        rounded
        size="small"
        :aria-label="t('payments.table.previousYear')"
        :disabled="year <= MIN_YEAR"
        @click="changeYear(-1)"
      />
      <span class="tenant-payment-table__year" aria-live="polite">{{ year }}</span>
      <Button
        class="tenant-payment-table__next"
        icon="pi pi-chevron-right"
        severity="secondary"
        text
        rounded
        size="small"
        :aria-label="t('payments.table.nextYear')"
        :disabled="year >= maxYear"
        @click="changeYear(1)"
      />
    </div>

    <p v-if="isLoading && tenants.length === 0" class="tenant-payment-table__status" role="status">
      {{ t('payments.table.loading') }}
    </p>
    <p v-else-if="tenants.length === 0" class="tenant-payment-table__empty">{{ t('payments.table.empty') }}</p>

    <div
      v-else
      class="tenant-payment-table__scroll"
      role="region"
      tabindex="0"
      :aria-label="t('payments.table.regionLabel', { year })"
    >
      <table class="tenant-payment-table__table">
        <thead>
          <tr>
            <th scope="col" class="tenant-payment-table__tenant-head">{{ t('payments.table.tenantColumn') }}</th>
            <th v-for="(label, index) in monthLabels" :key="MONTHS[index]" scope="col" class="tenant-payment-table__month-head">
              {{ label }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="tenant in tenants"
            :key="tenant.tenantId"
            class="tenant-payment-table__row"
            :class="{ 'tenant-payment-table__row--alert': tenant.alert !== 'NONE' }"
          >
            <th scope="row" class="tenant-payment-table__tenant">
              <div class="tenant-payment-table__tenant-info">
                <span class="tenant-payment-table__tenant-name">{{ fullName(tenant) }}</span>
                <span class="tenant-payment-table__tenant-meta">
                  {{ formatFloor(tenant.floor) }} · {{ formatMoney(tenant.monthlyRent) }}
                </span>
                <PaymentAlertBadge :alert="tenant.alert" />
              </div>
            </th>
            <td v-for="month in tenant.months" :key="month.month" class="tenant-payment-table__cell">
              <PaymentStatusBadge :status="month.status" :detail="paidDetail(month)" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <ul class="tenant-payment-table__legend" :aria-label="t('payments.table.legend')">
      <li v-for="status in MONTH_STATUSES" :key="status" class="tenant-payment-table__legend-item">
        <PaymentStatusBadge :status="status" />
        <span>{{ statusLabel(status) }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped src="@/styles/payments/tenant-payment-table.css"></style>
