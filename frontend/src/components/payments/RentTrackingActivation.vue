<script setup lang="ts">
import { computed, onBeforeUnmount } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { usePaymentErrorText } from '@/composables/usePaymentErrorText'
import { useTrackingDeactivationConfirmation } from '@/composables/useTrackingDeactivationConfirmation'
import { usePaymentsStore } from '@/stores/payments'
import { formatNumericMonthYear } from '@/utils/paymentPeriods'

const props = defineProps<{ propertyId: string; trackingStart: string | null }>()

const { t } = useI18n()
const store = usePaymentsStore()
const { describeError } = usePaymentErrorText()
const { confirmDeactivation } = useTrackingDeactivationConfirmation()

const errorText = computed(() =>
  store.activationError ? describeError(store.activationError, 'payments.tracking.error') : null,
)
const deactivationErrorText = computed(() =>
  store.deactivationError ? describeError(store.deactivationError, 'payments.tracking.deactivateError') : null,
)

/** Activates the rent payment check for the property. */
async function activate() {
  store.clearActivationError()
  await store.activateTracking(props.propertyId)
}

/** Asks for confirmation, then deactivates the rent payment check for the property. */
function requestDeactivation() {
  store.clearDeactivationError()
  confirmDeactivation(() => store.deactivateTracking(props.propertyId))
}

onBeforeUnmount(() => {
  store.clearActivationError()
  store.clearDeactivationError()
})
</script>

<template>
  <div class="rent-tracking-activation">
    <template v-if="trackingStart">
      <p class="rent-tracking-activation__active">
        <i class="pi pi-check-circle" aria-hidden="true"></i>
        {{ t('payments.tracking.activeSince', { month: formatNumericMonthYear(trackingStart) }) }}
      </p>
      <Button
        class="rent-tracking-activation__deactivate"
        icon="pi pi-power-off"
        :label="store.isDeactivating ? t('payments.tracking.deactivating') : t('payments.tracking.deactivate')"
        :loading="store.isDeactivating"
        :disabled="store.isDeactivating"
        severity="secondary"
        outlined
        size="small"
        @click="requestDeactivation"
      />
    </template>
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
    <p v-if="deactivationErrorText" class="rent-tracking-activation__error" role="alert">{{ deactivationErrorText }}</p>
  </div>
</template>

<style scoped src="@/styles/payments/rent-tracking-activation.css"></style>
