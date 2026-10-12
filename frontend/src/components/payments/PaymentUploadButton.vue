<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { usePaymentErrorText } from '@/composables/usePaymentErrorText'
import { usePaymentsStore } from '@/stores/payments'
import { findStatementFileProblem, type StatementFileProblem } from '@/utils/statementFileValidation'

const props = defineProps<{ propertyId: string }>()

const { t } = useI18n()
const store = usePaymentsStore()
const { describeError } = usePaymentErrorText()

const fileInput = ref<HTMLInputElement | null>(null)
const fileProblem = ref<StatementFileProblem | null>(null)

const errorText = computed(() => {
  if (fileProblem.value) return t(`payments.upload.problem.${fileProblem.value}`)
  return store.scanError ? describeError(store.scanError, 'payments.upload.error') : null
})

/** Opens the browser's file picker. */
function openFilePicker() {
  fileInput.value?.click()
}

/** Checks the chosen file and, if it is a usable PDF, sends it to be scanned. */
async function onFileChosen(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  store.clearScanError()
  fileProblem.value = findStatementFileProblem(file)
  if (fileProblem.value) return
  await store.scanStatement(props.propertyId, file)
}

onBeforeUnmount(() => store.clearScanError())
</script>

<template>
  <div class="payment-upload-button">
    <input
      ref="fileInput"
      class="payment-upload-button__input"
      type="file"
      accept="application/pdf"
      hidden
      @change="onFileChosen"
    />
    <Button
      class="payment-upload-button__button"
      icon="pi pi-upload"
      :label="store.isScanning ? t('payments.upload.scanning') : t('payments.upload.button')"
      :loading="store.isScanning"
      :disabled="store.isScanning"
      severity="secondary"
      size="small"
      @click="openFilePicker"
    />
    <p class="payment-upload-button__hint">{{ t('payments.upload.hint') }}</p>
    <p v-if="store.isScanning" class="payment-upload-button__status" role="status">
      {{ t('payments.upload.scanningHint') }}
    </p>
    <p v-if="errorText" class="payment-upload-button__error" role="alert">{{ errorText }}</p>
  </div>
</template>

<style scoped src="@/styles/payments/payment-upload-button.css"></style>
