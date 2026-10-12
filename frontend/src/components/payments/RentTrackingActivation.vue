<script setup lang="ts">
import { computed, onBeforeUnmount } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'
import { usePaymentErrorText } from '@/composables/usePaymentErrorText'
import { usePaymentsStore } from '@/stores/payments'
import { formatNumericMonthYear } from '@/utils/paymentPeriods'

const props = defineProps<{ propertyId: string; trackingStart: string | null }>()

const { t } = useI18n()
const { currentLocale } = useLocale()
const store = usePaymentsStore()
const { describeError } = usePaymentErrorText()

const errorText = computed(() =>
  store.activationError ? describeError(store.activationError, 'payments.tracking.error') : null,
)

/** Activates the rent payment check for the property. */
async function activate() {
  store.clearActivationError()
  await store.activateTracking(props.propertyId)
}

onBeforeUnmount(() => store.clearActivationError())
</script>

<template>
  <div class="rent-tracking-activation">
    <p v-if="trackingStart" class="rent-tracking-activation__active">
      <i class="pi pi-check-circle" aria-hidden="true"></i>
      {{ t('payments.tracking.activeSince', { month: formatNumericMonthYear(trackingStart, currentLocale) }) }}
    </p>
    <template v-else>
      <p class="rent-tracking-activation__explanation">{{ t('payments.tracking.explanation') }}</p>
      <Button
        class="rent-tracking-activation__button"
        icon="pi pi-power-off"
        :label="store.isActivating ? t('payments.tracking.activating') : t('payments.tracking.activate')"
        :loading="store.isActivating"
        :disabled="store.isActivating"
        size="small"
        @click="activate"
      />
    </template>
    <p v-if="errorText" class="rent-tracking-activation__error" role="alert">{{ errorText }}</p>
  </div>
</template>

<style scoped src="@/styles/payments/rent-tracking-activation.css"></style>
