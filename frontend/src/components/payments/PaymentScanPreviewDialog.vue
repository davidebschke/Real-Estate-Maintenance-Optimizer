<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import { useToast } from 'primevue/usetoast'
import { useI18n } from 'vue-i18n'
import { useApartmentFormatting } from '@/composables/useApartmentFormatting'
import { useLocale } from '@/composables/useLocale'
import { usePaymentErrorText } from '@/composables/usePaymentErrorText'
import { usePaymentStatus } from '@/composables/usePaymentStatus'
import { usePaymentsStore } from '@/stores/payments'
import { useTenantsStore } from '@/stores/tenants'
import { formatLocalizedIsoDate } from '@/utils/dateFormat'
import { generatePeriodOptions, parsePeriodKey, periodKey } from '@/utils/paymentPeriods'
import type { PaymentToSave, ScannedPayment } from '@/types/payment'

/** How long the success message stays visible in milliseconds. */
const SUCCESS_TOAST_LIFE_MS = 5000

/** One scanned payment together with the user's decisions about it. */
interface PreviewRow {
  key: number
  payment: ScannedPayment
  include: boolean
  tenantId: string | null
  period: string
  /** Whether the user picked the tenant themselves, which ends the highlighting of an uncertain suggestion. */
  reviewed: boolean
}

const visible = defineModel<boolean>('visible', { required: true })

const { t } = useI18n()
const { currentLocale } = useLocale()
const { formatMoney, formatFloor } = useApartmentFormatting()
const { matchLabel, matchSeverity } = usePaymentStatus()
const { describeError } = usePaymentErrorText()
const paymentsStore = usePaymentsStore()
const tenantsStore = useTenantsStore()
const toast = useToast()

const rows = ref<PreviewRow[]>([])

const propertyId = computed(() => paymentsStore.scanSession?.propertyId ?? null)

const tenantOptions = computed(() => {
  const apartments = propertyId.value ? (tenantsStore.apartmentsByProperty[propertyId.value] ?? []) : []
  return apartments.flatMap((apartment) =>
    apartment.tenants.map((tenant) => ({
      value: tenant.id,
      label: `${tenant.firstName} ${tenant.lastName} (${formatFloor(apartment.floor)})`,
    })),
  )
})

const periodOptions = computed(() =>
  generatePeriodOptions(
    new Date().getFullYear(),
    currentLocale.value,
    (paymentsStore.scanSession?.payments ?? []).map((payment) => periodKey(payment.periodYear, payment.periodMonth)),
  ),
)

const includedRows = computed(() => rows.value.filter((row) => row.include && row.tenantId !== null))
const reviewCount = computed(() => rows.value.filter(needsReview).length)

watch(
  () => paymentsStore.scanSession,
  (session) => {
    rows.value = (session?.payments ?? []).map(toRow)
  },
  { immediate: true },
)

/** Builds a row from a scanned payment, selecting it unless it was saved before or has no tenant yet. */
function toRow(payment: ScannedPayment, index: number): PreviewRow {
  return {
    key: index,
    payment,
    include: !payment.alreadyRecorded && payment.match === 'MATCH',
    tenantId: payment.tenantId,
    period: periodKey(payment.periodYear, payment.periodMonth),
    reviewed: false,
  }
}

/** Whether the tenant of a row was only suggested uncertainly or not at all and the user has not chosen one yet. */
function needsReview(row: PreviewRow): boolean {
  return row.payment.match !== 'MATCH' && !row.reviewed
}

/** Returns the label and severity of the assignment badge of a row. */
function matchBadge(row: PreviewRow) {
  if (row.reviewed) return { label: t('payments.match.MANUAL'), severity: 'info' as const }
  return { label: matchLabel(row.payment.match), severity: matchSeverity(row.payment.match) }
}

/** Assigns the chosen tenant to a row and selects it for saving, since choosing a tenant shows the intent to save. */
function assignTenant(row: PreviewRow, tenantId: string) {
  row.tenantId = tenantId
  row.reviewed = true
  row.include = true
}

/** Builds the save request body of a selected row. */
function toPaymentToSave(row: PreviewRow): PaymentToSave {
  const { year, month } = parsePeriodKey(row.period)
  return {
    tenantId: row.tenantId!,
    paymentDate: row.payment.paymentDate,
    amount: row.payment.amount,
    reference: row.payment.reference,
    periodYear: year,
    periodMonth: month,
  }
}

/** Saves the selected payments and reports how many were stored; the preview closes itself once the store dropped the scan. */
async function save() {
  if (!propertyId.value || includedRows.value.length === 0 || paymentsStore.isSaving) return
  const result = await paymentsStore.savePayments(propertyId.value, includedRows.value.map(toPaymentToSave))
  if (!result) return
  toast.add({
    severity: 'success',
    summary: t('payments.preview.savedTitle'),
    detail: t('payments.preview.savedMessage', { saved: result.savedCount, skipped: result.skippedCount }),
    life: SUCCESS_TOAST_LIFE_MS,
  })
}

/** Closes the preview without saving anything. */
function cancel() {
  visible.value = false
}
</script>

<template>
  <Dialog v-model:visible="visible" modal :header="t('payments.preview.title')" class="payment-scan-preview-dialog">
    <p class="payment-scan-preview-dialog__summary" role="status">
      {{ t('payments.preview.summary', { count: rows.length }) }}
    </p>
    <p v-if="reviewCount > 0" class="payment-scan-preview-dialog__review-hint">
      <i class="pi pi-exclamation-triangle" aria-hidden="true"></i>
      {{ t('payments.preview.reviewHint', { count: reviewCount }) }}
    </p>

    <p v-if="rows.length === 0" class="payment-scan-preview-dialog__empty">{{ t('payments.preview.empty') }}</p>

    <template v-else>
      <div class="payment-scan-preview-dialog__head" aria-hidden="true">
        <span></span>
        <span>{{ t('payments.preview.dateColumn') }}</span>
        <span>{{ t('payments.preview.amountColumn') }}</span>
        <span>{{ t('payments.preview.payerColumn') }}</span>
        <span>{{ t('payments.preview.tenantColumn') }}</span>
        <span>{{ t('payments.preview.periodColumn') }}</span>
      </div>

      <ul class="payment-scan-preview-dialog__rows">
        <li
          v-for="row in rows"
          :key="row.key"
          class="payment-scan-preview-dialog__row"
          :class="{
            'payment-scan-preview-dialog__row--review': needsReview(row),
            'payment-scan-preview-dialog__row--excluded': !row.include,
          }"
        >
          <div class="payment-scan-preview-dialog__cell payment-scan-preview-dialog__cell--include">
            <Checkbox
              v-model="row.include"
              binary
              :input-id="`payment-include-${row.key}`"
              :aria-label="t('payments.preview.includeRow', { payer: row.payment.payerName })"
              :disabled="row.tenantId === null"
            />
          </div>

          <div class="payment-scan-preview-dialog__cell payment-scan-preview-dialog__cell--date">
            <span class="payment-scan-preview-dialog__cell-label">{{ t('payments.preview.dateColumn') }}</span>
            <span class="payment-scan-preview-dialog__date">{{ formatLocalizedIsoDate(row.payment.paymentDate, currentLocale) }}</span>
          </div>

          <div class="payment-scan-preview-dialog__cell payment-scan-preview-dialog__cell--amount">
            <span class="payment-scan-preview-dialog__cell-label">{{ t('payments.preview.amountColumn') }}</span>
            <span class="payment-scan-preview-dialog__amount">{{ formatMoney(row.payment.amount) }}</span>
          </div>

          <div class="payment-scan-preview-dialog__cell payment-scan-preview-dialog__cell--payer">
            <span class="payment-scan-preview-dialog__cell-label">{{ t('payments.preview.payerColumn') }}</span>
            <span class="payment-scan-preview-dialog__payer">{{ row.payment.payerName }}</span>
            <span class="payment-scan-preview-dialog__reference">{{ row.payment.reference }}</span>
            <span v-if="row.payment.alreadyRecorded" class="payment-scan-preview-dialog__recorded">
              <i class="pi pi-check" aria-hidden="true"></i>
              {{ t('payments.preview.alreadyRecorded') }}
            </span>
          </div>

          <div class="payment-scan-preview-dialog__cell payment-scan-preview-dialog__cell--tenant">
            <span class="payment-scan-preview-dialog__cell-label">{{ t('payments.preview.tenantColumn') }}</span>
            <Select
              :model-value="row.tenantId"
              :options="tenantOptions"
              option-label="label"
              option-value="value"
              :input-id="`payment-tenant-${row.key}`"
              :aria-label="t('payments.preview.tenantSelect', { payer: row.payment.payerName })"
              :placeholder="t('payments.preview.tenantPlaceholder')"
              :invalid="needsReview(row)"
              class="payment-scan-preview-dialog__select payment-scan-preview-dialog__tenant-select"
              @update:model-value="assignTenant(row, $event)"
            />
            <span
              class="payment-scan-preview-dialog__match"
              :class="`payment-severity--${matchBadge(row).severity}`"
            >
              {{ matchBadge(row).label }}
            </span>
          </div>

          <div class="payment-scan-preview-dialog__cell payment-scan-preview-dialog__cell--period">
            <span class="payment-scan-preview-dialog__cell-label">{{ t('payments.preview.periodColumn') }}</span>
            <Select
              v-model="row.period"
              :options="periodOptions"
              option-label="label"
              option-value="value"
              :input-id="`payment-period-${row.key}`"
              :aria-label="t('payments.preview.periodSelect', { payer: row.payment.payerName })"
              class="payment-scan-preview-dialog__select payment-scan-preview-dialog__period-select"
            />
          </div>
        </li>
      </ul>
    </template>

    <p v-if="paymentsStore.saveError" class="payment-scan-preview-dialog__error" role="alert">
      {{ describeError(paymentsStore.saveError, 'payments.preview.saveError') }}
    </p>

    <div class="payment-scan-preview-dialog__actions">
      <Button
        class="payment-scan-preview-dialog__save"
        :label="t('payments.preview.save', { count: includedRows.length })"
        :disabled="includedRows.length === 0 || paymentsStore.isSaving"
        :loading="paymentsStore.isSaving"
        @click="save"
      />
      <button type="button" class="payment-scan-preview-dialog__cancel" @click="cancel">
        {{ t('payments.preview.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/payments/payment-severity.css"></style>
<style src="@/styles/payments/payment-scan-preview-dialog.css"></style>
