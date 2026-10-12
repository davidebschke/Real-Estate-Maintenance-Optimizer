<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import DemoQuotaHint from '@/components/auth/DemoQuotaHint.vue'
import TenantNameFields from '@/components/tenants/TenantNameFields.vue'
import { useTenantsStore } from '@/stores/tenants'
import { useLocale } from '@/composables/useLocale'
import { useDemoQuota } from '@/composables/useDemoQuota'
import { useTouchedFields } from '@/composables/useTouchedFields'
import { AMOUNT_MAX, AREA_MAX, FLOOR_MAX, FLOOR_MIN, toValidApartmentDetails } from '@/utils/apartmentFieldValidation'
import { currentMonthKey, listRentEffectiveMonths } from '@/utils/rentEffectiveMonths'

const MONEY_FIELDS = ['totalRent', 'coldRent', 'additionalCosts'] as const

const visible = defineModel<boolean>('visible', { required: true })

const { t } = useI18n()
const { currentLocale } = useLocale()
const store = useTenantsStore()

const form = reactive({
  floor: null as number | null,
  areaSquareMeters: null as number | null,
  totalRent: null as number | null,
  coldRent: null as number | null,
  additionalCosts: null as number | null,
  firstName: '',
  lastName: '',
  rentEffectiveMonth: '',
})

const { touched, markTouched, resetTouched } = useTouchedFields([
  'floor',
  'areaSquareMeters',
  'totalRent',
  'coldRent',
  'additionalCosts',
  'firstName',
  'lastName',
])

/** Whether a save request is in flight, which blocks submitting the same form a second time. */
const isSubmitting = ref(false)

/** The moment the form was opened, from which the selectable months of a rent change are derived. */
const openedAt = ref(new Date())

const isEditMode = computed(() => store.apartmentFormTarget?.mode === 'edit')

const { isExhausted: isCreationQuotaExhausted } = useDemoQuota('tenants')

const title = computed(() => t(isEditMode.value ? 'tenants.apartmentForm.editTitle' : 'tenants.apartmentForm.createTitle'))
const submitLabel = computed(() => t(isEditMode.value ? 'tenants.apartmentForm.submitEdit' : 'tenants.apartmentForm.submitCreate'))

const apartmentDetails = computed(() => toValidApartmentDetails(form))

const isFirstNameEmpty = computed(() => form.firstName.trim().length === 0)
const isLastNameEmpty = computed(() => form.lastName.trim().length === 0)
const hasFirstTenantInput = computed(() => !isFirstNameEmpty.value || !isLastNameEmpty.value)
const isFirstTenantComplete = computed(() => !isFirstNameEmpty.value && !isLastNameEmpty.value)

/** Whether the first tenant section is either empty (a vacant apartment) or filled completely. */
const isFirstTenantValid = computed(() => isEditMode.value || !hasFirstTenantInput.value || isFirstTenantComplete.value)

/** Whether a demo account used up its tenant creations while the form would still create a tenant, which an empty apartment never does. */
const isBlockedByQuota = computed(() => !isEditMode.value && hasFirstTenantInput.value && isCreationQuotaExhausted.value)

const isValid = computed(() => apartmentDetails.value !== null && isFirstTenantValid.value)

/** Whether the total rent of an edited apartment differs from its stored one, which makes the month of the change a required choice. */
const isRentChanged = computed(() => {
  const target = store.apartmentFormTarget
  return target?.mode === 'edit' && form.totalRent !== null && form.totalRent !== target.apartment.totalRent
})

const rentEffectiveMonthOptions = computed(() => listRentEffectiveMonths(openedAt.value, currentLocale.value))

const showFirstNameError = computed(
  () => !isEditMode.value && isFirstNameEmpty.value && !isLastNameEmpty.value && (touched.firstName || touched.lastName),
)
const showLastNameError = computed(
  () => !isEditMode.value && isLastNameEmpty.value && !isFirstNameEmpty.value && (touched.firstName || touched.lastName),
)
const isFloorMissing = computed(() => touched.floor && form.floor === null)
const isAreaMissing = computed(
  () => touched.areaSquareMeters && (form.areaSquareMeters === null || form.areaSquareMeters <= 0),
)

/** Whether the given money field has been left empty after being touched. */
function isMoneyFieldMissing(field: (typeof MONEY_FIELDS)[number]): boolean {
  return touched[field] && form[field] === null
}

watch(visible, (isVisible) => {
  if (isVisible) resetForm()
})

/** Fills every field from the form target: blank for a new apartment, the stored data for an edit. */
function resetForm() {
  const target = store.apartmentFormTarget
  const apartment = target?.mode === 'edit' ? target.apartment : null
  openedAt.value = new Date()
  form.floor = apartment?.floor ?? null
  form.areaSquareMeters = apartment?.areaSquareMeters ?? null
  form.totalRent = apartment?.totalRent ?? null
  form.coldRent = apartment?.coldRent ?? null
  form.additionalCosts = apartment?.additionalCosts ?? null
  form.firstName = ''
  form.lastName = ''
  form.rentEffectiveMonth = currentMonthKey(openedAt.value)
  resetTouched()
}

/** Saves the form according to its mode, then closes the dialog on success. */
async function submit() {
  const target = store.apartmentFormTarget
  const apartment = apartmentDetails.value
  if (!isValid.value || !target || !apartment || isSubmitting.value || isBlockedByQuota.value) return

  isSubmitting.value = true
  let succeeded: boolean
  try {
    if (target.mode === 'create') {
      const tenant = { firstName: form.firstName.trim(), lastName: form.lastName.trim() }
      succeeded = await store.createApartment(
        target.propertyId,
        isFirstTenantComplete.value ? { apartment, tenant } : { apartment },
      )
    } else {
      succeeded = await store.updateApartment(target.apartment.propertyId, target.apartment.id, {
        apartment,
        rentEffectiveMonth: isRentChanged.value && form.rentEffectiveMonth !== currentMonthKey(openedAt.value) ? form.rentEffectiveMonth : null,
      })
    }
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
  <Dialog v-model:visible="visible" modal :header="title" class="apartment-form-dialog">
    <section class="apartment-form-dialog__section">
      <h3 class="apartment-form-dialog__section-heading">{{ t('tenants.apartmentForm.apartmentSection') }}</h3>

      <div class="apartment-form-dialog__grid">
        <div class="apartment-form-dialog__field">
          <RequiredFieldLabel field-id="apartment-floor" :label="t('tenants.apartmentForm.floorLabel')" />
          <InputNumber
            v-model="form.floor"
            input-id="apartment-floor"
            :min="FLOOR_MIN"
            :max="FLOOR_MAX"
            :max-fraction-digits="0"
            :use-grouping="false"
            :invalid="isFloorMissing"
            :locale="currentLocale"
            :placeholder="t('tenants.apartmentForm.floorPlaceholder')"
            aria-required="true"
            :aria-describedby="isFloorMissing ? 'apartment-floor-error' : 'apartment-floor-hint'"
            @blur="markTouched('floor')"
          />
          <p v-if="isFloorMissing" id="apartment-floor-error" class="apartment-form-dialog__field-error">
            {{ t('tenants.apartmentForm.floorRequiredError') }}
          </p>
          <p v-else id="apartment-floor-hint" class="apartment-form-dialog__hint">
            {{ t('tenants.apartmentForm.floorHint') }}
          </p>
        </div>

        <div class="apartment-form-dialog__field">
          <RequiredFieldLabel field-id="apartment-area" :label="t('tenants.apartmentForm.areaLabel')" />
          <InputNumber
            v-model="form.areaSquareMeters"
            input-id="apartment-area"
            suffix=" m²"
            :min="0"
            :max="AREA_MAX"
            :min-fraction-digits="0"
            :max-fraction-digits="2"
            :invalid="isAreaMissing"
            :locale="currentLocale"
            :placeholder="t('tenants.apartmentForm.areaPlaceholder')"
            aria-required="true"
            :aria-describedby="isAreaMissing ? 'apartment-area-error' : undefined"
            @blur="markTouched('areaSquareMeters')"
          />
          <p v-if="isAreaMissing" id="apartment-area-error" class="apartment-form-dialog__field-error">
            {{ t('tenants.apartmentForm.areaRequiredError') }}
          </p>
        </div>
      </div>

      <div class="apartment-form-dialog__grid apartment-form-dialog__grid--amounts">
        <div v-for="field in MONEY_FIELDS" :key="field" class="apartment-form-dialog__field">
          <RequiredFieldLabel :field-id="`apartment-${field}`" :label="t(`tenants.apartmentForm.${field}Label`)" />
          <InputNumber
            v-model="form[field]"
            :input-id="`apartment-${field}`"
            mode="currency"
            currency="EUR"
            :min="0"
            :max="AMOUNT_MAX"
            :invalid="isMoneyFieldMissing(field)"
            :locale="currentLocale"
            :placeholder="t(`tenants.apartmentForm.${field}Placeholder`)"
            aria-required="true"
            :aria-describedby="isMoneyFieldMissing(field) ? `apartment-${field}-error` : undefined"
            @blur="markTouched(field)"
          />
          <p v-if="isMoneyFieldMissing(field)" :id="`apartment-${field}-error`" class="apartment-form-dialog__field-error">
            {{ t('tenants.apartmentForm.amountRequiredError') }}
          </p>
        </div>
      </div>

      <div v-if="isRentChanged" class="apartment-form-dialog__field">
        <RequiredFieldLabel
          field-id="apartment-rent-effective-month"
          :label="t('tenants.apartmentForm.rentEffectiveMonthLabel')"
        />
        <Select
          v-model="form.rentEffectiveMonth"
          input-id="apartment-rent-effective-month"
          :options="rentEffectiveMonthOptions"
          option-label="label"
          option-value="value"
          aria-required="true"
          aria-describedby="apartment-rent-effective-month-hint"
        />
        <p id="apartment-rent-effective-month-hint" class="apartment-form-dialog__hint">
          {{ t('tenants.apartmentForm.rentEffectiveMonthHint') }}
        </p>
      </div>
    </section>

    <section v-if="!isEditMode" class="apartment-form-dialog__section">
      <h3 class="apartment-form-dialog__section-heading">{{ t('tenants.apartmentForm.firstTenantSection') }}</h3>
      <p class="apartment-form-dialog__hint">{{ t('tenants.apartmentForm.firstTenantHint') }}</p>
      <TenantNameFields
        v-model:first-name="form.firstName"
        v-model:last-name="form.lastName"
        id-prefix="apartment-tenant"
        :required="false"
        :show-first-name-error="showFirstNameError"
        :show-last-name-error="showLastNameError"
        @blur-first-name="markTouched('firstName')"
        @blur-last-name="markTouched('lastName')"
      />
    </section>

    <p v-if="store.hasSaveError" class="apartment-form-dialog__error">
      {{ store.lastChangeErrorMessage ?? t('tenants.apartmentForm.error') }}
    </p>

    <DemoQuotaHint v-if="!isEditMode" resource="tenants" />

    <p class="apartment-form-dialog__required-legend">{{ t('tenants.apartmentForm.requiredFieldsLegend') }}</p>

    <div class="apartment-form-dialog__actions">
      <Button :label="submitLabel" :disabled="!isValid || isSubmitting || isBlockedByQuota" @click="submit" />
      <button type="button" class="apartment-form-dialog__cancel" @click="cancel">
        {{ t('tenants.apartmentForm.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/tenants/apartment-form-dialog.css"></style>
