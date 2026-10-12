<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useApartmentFormatting } from '@/composables/useApartmentFormatting'
import { useLocale } from '@/composables/useLocale'
import { formatLocalizedIsoDate } from '@/utils/dateFormat'
import { formatMonthYear } from '@/utils/paymentPeriods'
import type { FormerTenantPayment } from '@/types/payment'

defineProps<{ payments: FormerTenantPayment[] }>()

const { t } = useI18n()
const { currentLocale } = useLocale()
const { formatFloor, formatMoney } = useApartmentFormatting()
</script>

<template>
  <section class="former-tenant-payment-list">
    <h4 class="former-tenant-payment-list__heading">{{ t('payments.former.heading') }}</h4>
    <table class="former-tenant-payment-list__table">
      <thead>
        <tr>
          <th scope="col">{{ t('payments.former.dateColumn') }}</th>
          <th scope="col">{{ t('payments.former.amountColumn') }}</th>
          <th scope="col">{{ t('payments.former.periodColumn') }}</th>
          <th scope="col">{{ t('payments.former.apartmentColumn') }}</th>
          <th scope="col">{{ t('payments.former.noteColumn') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(payment, index) in payments"
          :key="`${payment.apartmentId}-${payment.paymentDate}-${payment.periodMonth}-${index}`"
          class="former-tenant-payment-list__row"
        >
          <td class="former-tenant-payment-list__date" :data-label="t('payments.former.dateColumn')">
            {{ formatLocalizedIsoDate(payment.paymentDate, currentLocale) }}
          </td>
          <td class="former-tenant-payment-list__amount" :data-label="t('payments.former.amountColumn')">
            {{ formatMoney(payment.amount) }}
          </td>
          <td class="former-tenant-payment-list__period" :data-label="t('payments.former.periodColumn')">
            {{ formatMonthYear(payment.periodYear, payment.periodMonth, currentLocale) }}
          </td>
          <td class="former-tenant-payment-list__apartment" :data-label="t('payments.former.apartmentColumn')">
            {{ formatFloor(payment.floor) }}
          </td>
          <td class="former-tenant-payment-list__note" :data-label="t('payments.former.noteColumn')">
            {{ t('payments.former.note', { name: payment.formerTenantName }) }}
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped src="@/styles/payments/former-tenant-payment-list.css"></style>
