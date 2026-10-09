<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import { useTenantsStore } from '@/stores/tenants'
import { useLocale } from '@/composables/useLocale'
import { useTouchedFields } from '@/composables/useTouchedFields'
import type { ApartmentDetails } from '@/types/apartment'

const NAME_MAX_LENGTH = 50
const FLOOR_MIN = -3
const FLOOR_MAX = 100
const AREA_MAX = 99999.99
const AMOUNT_MAX = 9999999.99
const MONEY_FIELDS = ['totalRent', 'coldRent', 'additionalCosts'] as const
const TITLE_KEYS = {
  create: 'tenants.form.createTitle',
  add: 'tenants.form.addTitle',
  edit: 'tenants.form.editTitle',
} as const
const SUBMIT_LABEL_KEYS = {
  create: 'tenants.form.submitCreate',
  add: 'tenants.form.submitAdd',
  edit: 'tenants.form.submitEdit',
} as const

const visible = defineModel<boolean>('visible', { required: true })

const { t } = useI18n()
const { currentLocale } = useLocale()
const store = useTenantsStore()

const form = reactive({
  firstName: '',
  lastName: '',
  floor: null as number | null,
  areaSquareMeters: null as number | null,
  totalRent: null as number | null,
  coldRent: null as number | null,
  additionalCosts: null as number | null,
})

const { touched, markTouched, resetTouched } = useTouchedFields([
  'firstName',
  'lastName',
  'floor',
  'areaSquareMeters',
  'totalRent',
  'coldRent',
  'additionalCosts',
])

const mode = computed(() => store.formTarget?.mode ?? 'create')

/** Whether the apartment fields only display the apartment's data, which is shared and edited through its existing tenants. */
const isApartmentReadOnly = computed(() => mode.value === 'add')

const title = computed(() => t(TITLE_KEYS[mode.value]))
const submitLabel = computed(() => t(SUBMIT_LABEL_KEYS[mode.value]))

/** The apartment fields as one object, or null while any of them is missing or outside its allowed range. */
const apartmentDetails = computed<ApartmentDetails | null>(() => {
  const { floor, areaSquareMeters, totalRent, coldRent, additionalCosts } = form
  if (floor === null || areaSquareMeters === null || totalRent === null || coldRent === null || additionalCosts === null) {
    return null
  }
  const isFloorValid = Number.isInteger(floor) && floor >= FLOOR_MIN && floor <= FLOOR_MAX
  const isAreaValid = areaSquareMeters > 0 && areaSquareMeters <= AREA_MAX
  const areAmountsValid = [totalRent, coldRent, additionalCosts].every((amount) => amount >= 0 && amount <= AMOUNT_MAX)
  return isFloorValid && isAreaValid && areAmountsValid
    ? { floor, areaSquareMeters, totalRent, coldRent, additionalCosts }
    : null
})

const isValid = computed(
  () => form.firstName.trim().length > 0 && form.lastName.trim().length > 0 && apartmentDetails.value !== null,
)

const isFirstNameMissing = computed(() => touched.firstName && form.firstName.trim().length === 0)
const isLastNameMissing = computed(() => touched.lastName && form.lastName.trim().length === 0)
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

/** Fills every field from the form target: blank for a new tenant with a new apartment, only the apartment for a further tenant, everything for an edit. */
function resetForm() {
  const target = store.formTarget
  const apartment = target && target.mode !== 'create' ? target.apartment : null
  const tenant = target?.mode === 'edit' ? target.tenant : null
  form.firstName = tenant?.firstName ?? ''
  form.lastName = tenant?.lastName ?? ''
  form.floor = apartment?.floor ?? null
  form.areaSquareMeters = apartment?.areaSquareMeters ?? null
  form.totalRent = apartment?.totalRent ?? null
  form.coldRent = apartment?.coldRent ?? null
  form.additionalCosts = apartment?.additionalCosts ?? null
  resetTouched()
}

/** Saves the form according to its mode, then closes the dialog on success. */
async function submit() {
  const target = store.formTarget
  const apartment = apartmentDetails.value
  if (!isValid.value || !target || !apartment) return

  const tenant = { firstName: form.firstName.trim(), lastName: form.lastName.trim() }
  let succeeded: boolean
  if (target.mode === 'create') {
    succeeded = await store.createApartment(target.propertyId, { apartment, tenant })
  } else if (target.mode === 'add') {
    succeeded = await store.addTenant(target.apartment.propertyId, target.apartment.id, tenant)
  } else {
    succeeded = await store.updateTenant(target.apartment.propertyId, target.tenant.id, { apartment, tenant })
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
    <section class="tenant-form-dialog__section">
      <h3 class="tenant-form-dialog__section-heading">{{ t('tenants.form.tenantSection') }}</h3>
      <div class="tenant-form-dialog__grid">
        <div class="tenant-form-dialog__field">
          <RequiredFieldLabel field-id="tenant-first-name" :label="t('tenants.form.firstNameLabel')" />
          <InputText
            id="tenant-first-name"
            v-model="form.firstName"
            :maxlength="NAME_MAX_LENGTH"
            :invalid="isFirstNameMissing"
            aria-required="true"
            :aria-describedby="isFirstNameMissing ? 'tenant-first-name-error' : undefined"
            :placeholder="t('tenants.form.firstNamePlaceholder')"
            @blur="markTouched('firstName')"
          />
          <p v-if="isFirstNameMissing" id="tenant-first-name-error" class="tenant-form-dialog__field-error">
            {{ t('tenants.form.firstNameRequiredError') }}
          </p>
        </div>

        <div class="tenant-form-dialog__field">
          <RequiredFieldLabel field-id="tenant-last-name" :label="t('tenants.form.lastNameLabel')" />
          <InputText
            id="tenant-last-name"
            v-model="form.lastName"
            :maxlength="NAME_MAX_LENGTH"
            :invalid="isLastNameMissing"
            aria-required="true"
            :aria-describedby="isLastNameMissing ? 'tenant-last-name-error' : undefined"
            :placeholder="t('tenants.form.lastNamePlaceholder')"
            @blur="markTouched('lastName')"
          />
          <p v-if="isLastNameMissing" id="tenant-last-name-error" class="tenant-form-dialog__field-error">
            {{ t('tenants.form.lastNameRequiredError') }}
          </p>
        </div>
      </div>
    </section>

    <section class="tenant-form-dialog__section">
      <h3 class="tenant-form-dialog__section-heading">{{ t('tenants.form.apartmentSection') }}</h3>
      <p v-if="mode !== 'create'" class="tenant-form-dialog__hint">{{ t('tenants.form.sharedApartmentHint') }}</p>

      <div class="tenant-form-dialog__grid">
        <div class="tenant-form-dialog__field">
          <RequiredFieldLabel field-id="tenant-floor" :label="t('tenants.form.floorLabel')" />
          <InputNumber
            v-model="form.floor"
            input-id="tenant-floor"
            :min="FLOOR_MIN"
            :max="FLOOR_MAX"
            :use-grouping="false"
            :disabled="isApartmentReadOnly"
            :invalid="isFloorMissing"
            :locale="currentLocale"
            :placeholder="t('tenants.form.floorPlaceholder')"
            aria-required="true"
            :aria-describedby="isFloorMissing ? 'tenant-floor-error' : 'tenant-floor-hint'"
            @blur="markTouched('floor')"
          />
          <p v-if="isFloorMissing" id="tenant-floor-error" class="tenant-form-dialog__field-error">
            {{ t('tenants.form.floorRequiredError') }}
          </p>
          <p v-else id="tenant-floor-hint" class="tenant-form-dialog__hint">{{ t('tenants.form.floorHint') }}</p>
        </div>

        <div class="tenant-form-dialog__field">
          <RequiredFieldLabel field-id="tenant-area" :label="t('tenants.form.areaLabel')" />
          <InputNumber
            v-model="form.areaSquareMeters"
            input-id="tenant-area"
            suffix=" m²"
            :min="0"
            :max="AREA_MAX"
            :min-fraction-digits="0"
            :max-fraction-digits="2"
            :disabled="isApartmentReadOnly"
            :invalid="isAreaMissing"
            :locale="currentLocale"
            :placeholder="t('tenants.form.areaPlaceholder')"
            aria-required="true"
            :aria-describedby="isAreaMissing ? 'tenant-area-error' : undefined"
            @blur="markTouched('areaSquareMeters')"
          />
          <p v-if="isAreaMissing" id="tenant-area-error" class="tenant-form-dialog__field-error">
            {{ t('tenants.form.areaRequiredError') }}
          </p>
        </div>
      </div>

      <div class="tenant-form-dialog__grid tenant-form-dialog__grid--amounts">
        <div v-for="field in MONEY_FIELDS" :key="field" class="tenant-form-dialog__field">
          <RequiredFieldLabel :field-id="`tenant-${field}`" :label="t(`tenants.form.${field}Label`)" />
          <InputNumber
            v-model="form[field]"
            :input-id="`tenant-${field}`"
            mode="currency"
            currency="EUR"
            :min="0"
            :max="AMOUNT_MAX"
            :disabled="isApartmentReadOnly"
            :invalid="isMoneyFieldMissing(field)"
            :locale="currentLocale"
            :placeholder="t(`tenants.form.${field}Placeholder`)"
            aria-required="true"
            :aria-describedby="isMoneyFieldMissing(field) ? `tenant-${field}-error` : undefined"
            @blur="markTouched(field)"
          />
          <p v-if="isMoneyFieldMissing(field)" :id="`tenant-${field}-error`" class="tenant-form-dialog__field-error">
            {{ t('tenants.form.amountRequiredError') }}
          </p>
        </div>
      </div>
    </section>

    <p v-if="store.hasSaveError" class="tenant-form-dialog__error">{{ t('tenants.form.error') }}</p>

    <p class="tenant-form-dialog__required-legend">{{ t('tenants.form.requiredFieldsLegend') }}</p>

    <div class="tenant-form-dialog__actions">
      <Button :label="submitLabel" :disabled="!isValid" @click="submit" />
      <button type="button" class="tenant-form-dialog__cancel" @click="cancel">
        {{ t('tenants.form.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/tenants/tenant-form-dialog.css"></style>
