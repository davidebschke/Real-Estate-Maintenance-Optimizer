<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import DemoQuotaHint from '@/components/auth/DemoQuotaHint.vue'
import TenantNameFields from '@/components/tenants/TenantNameFields.vue'
import { useTenantsStore } from '@/stores/tenants'
import { useDemoQuota } from '@/composables/useDemoQuota'
import { useTouchedFields } from '@/composables/useTouchedFields'

const TITLE_KEYS = { add: 'tenants.form.addTitle', edit: 'tenants.form.editTitle' } as const
const SUBMIT_LABEL_KEYS = { add: 'tenants.form.submitAdd', edit: 'tenants.form.submitEdit' } as const

const visible = defineModel<boolean>('visible', { required: true })

const { t } = useI18n()
const store = useTenantsStore()

const form = reactive({ firstName: '', lastName: '' })

const { touched, markTouched, resetTouched } = useTouchedFields(['firstName', 'lastName'])

/** Whether a save request is in flight, which blocks submitting the same form a second time. */
const isSubmitting = ref(false)

const mode = computed(() => store.tenantFormTarget?.mode ?? 'add')

const { isExhausted: isCreationQuotaExhausted } = useDemoQuota('tenants')

/** Whether a demo account already used up its tenant creations, which blocks adding but never renaming. */
const isBlockedByQuota = computed(() => mode.value === 'add' && isCreationQuotaExhausted.value)

const title = computed(() => t(TITLE_KEYS[mode.value]))
const submitLabel = computed(() => t(SUBMIT_LABEL_KEYS[mode.value]))

const isFirstNameEmpty = computed(() => form.firstName.trim().length === 0)
const isLastNameEmpty = computed(() => form.lastName.trim().length === 0)
const isValid = computed(() => !isFirstNameEmpty.value && !isLastNameEmpty.value)

watch(visible, (isVisible) => {
  if (isVisible) resetForm()
})

/** Fills the name fields from the form target: blank when adding a tenant, the tenant's current name when renaming. */
function resetForm() {
  const target = store.tenantFormTarget
  const tenant = target?.mode === 'edit' ? target.tenant : null
  form.firstName = tenant?.firstName ?? ''
  form.lastName = tenant?.lastName ?? ''
  resetTouched()
}

/** Saves the form according to its mode, then closes the dialog on success. */
async function submit() {
  const target = store.tenantFormTarget
  if (!isValid.value || !target || isSubmitting.value || isBlockedByQuota.value) return

  const tenant = { firstName: form.firstName.trim(), lastName: form.lastName.trim() }
  isSubmitting.value = true
  let succeeded: boolean
  try {
    succeeded =
      target.mode === 'add'
        ? await store.addTenant(target.apartment.propertyId, target.apartment.id, tenant)
        : await store.updateTenant(target.apartment.propertyId, target.tenant.id, tenant)
  } finally {
    isSubmitting.value = false
  }

  if (succeeded) visible.value = false
}

/** Closes the dialog without saving. */
function cancel() {
  visible.value = false
}
</script>

<template>
  <Dialog v-model:visible="visible" modal :header="title" class="tenant-form-dialog">
    <TenantNameFields
      v-model:first-name="form.firstName"
      v-model:last-name="form.lastName"
      id-prefix="tenant"
      :show-first-name-error="touched.firstName && isFirstNameEmpty"
      :show-last-name-error="touched.lastName && isLastNameEmpty"
      @blur-first-name="markTouched('firstName')"
      @blur-last-name="markTouched('lastName')"
    />

    <p v-if="store.hasSaveError" class="tenant-form-dialog__error">
      {{ store.lastChangeErrorMessage ?? t('tenants.form.error') }}
    </p>

    <DemoQuotaHint v-if="mode === 'add'" resource="tenants" />

    <p class="tenant-form-dialog__required-legend">{{ t('tenants.form.requiredFieldsLegend') }}</p>

    <div class="tenant-form-dialog__actions">
      <Button :label="submitLabel" :disabled="!isValid || isSubmitting || isBlockedByQuota" @click="submit" />
      <button type="button" class="tenant-form-dialog__cancel" @click="cancel">
        {{ t('tenants.form.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/tenants/tenant-form-dialog.css"></style>
