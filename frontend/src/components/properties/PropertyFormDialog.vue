<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import PropertyLocationPreviewMap from '@/components/properties/PropertyLocationPreviewMap.vue'
import { usePropertiesStore } from '@/stores/properties'
import {
  geocodeAddress,
  validateAddress,
  type AddressValidationResult,
  type GeocodedPosition,
} from '@/services/geocodingService'

const NAME_MAX_LENGTH = 50
const STREET_MAX_LENGTH = 100
const HOUSE_NUMBER_MAX_LENGTH = 10
const ADDRESS_SUPPLEMENT_MAX_LENGTH = 10
const GEOCODE_DEBOUNCE_MS = 500
const HOUSE_NUMBER_PATTERN = /^\d+$/
const POSTAL_CODE_PATTERN = /^\d{5}$/
/** Letters (incl. diacritics), digits, spaces and the punctuation that occurs in real German street names. */
const STREET_PATTERN = /^[\p{L}\d .'\-/]+$/u

const visible = defineModel<boolean>('visible', { required: true })

const { t } = useI18n()
const store = usePropertiesStore()

const form = reactive({
  name: '',
  street: '',
  houseNumber: '',
  addressSupplement: '',
  postalCode: '',
  city: '',
})

const geocodedPosition = ref<GeocodedPosition | null>(null)
const isGeocoding = ref(false)
let geocodeTimeout: ReturnType<typeof setTimeout> | null = null

const addressValidation = ref<AddressValidationResult | null>(null)
const isValidatingAddress = ref(false)
/** Incremented on every address-field change and validation attempt, to discard a stale in-flight validation response. */
let validationRequestId = 0

/** Whether the last address validation blocks submission, i.e. it found either a suggestion or no match at all. */
const hasBlockingAddressError = computed(() => addressValidation.value !== null)

/** The suggested address as a single readable line, for the "did you mean" hint. */
const suggestedAddressLine = computed(() => {
  if (!addressValidation.value || addressValidation.value.status !== 'SUGGESTION') return ''
  const { suggestedStreet, suggestedHouseNumber, suggestedPostalCode, suggestedCity } = addressValidation.value
  return `${suggestedStreet} ${suggestedHouseNumber}, ${suggestedPostalCode} ${suggestedCity}`
})

const isValid = computed(
  () =>
    form.name.trim().length > 0 &&
    form.name.length <= NAME_MAX_LENGTH &&
    form.street.trim().length > 0 &&
    STREET_PATTERN.test(form.street.trim()) &&
    HOUSE_NUMBER_PATTERN.test(form.houseNumber.trim()) &&
    POSTAL_CODE_PATTERN.test(form.postalCode) &&
    form.city.trim().length > 0 &&
    !isDuplicateName.value &&
    !isDuplicateAddress.value &&
    !hasBlockingAddressError.value,
)

/** Whether the street has been touched but contains a character that occurs in no real German street name. */
const isStreetFormatInvalid = computed(() => {
  const street = form.street.trim()
  return street.length > 0 && !STREET_PATTERN.test(street)
})

/** Whether the house number has been touched but is not purely digits. */
const isHouseNumberFormatInvalid = computed(() => {
  const houseNumber = form.houseNumber.trim()
  return houseNumber.length > 0 && !HOUSE_NUMBER_PATTERN.test(houseNumber)
})

/** Whether the postal code has been touched but is not exactly 5 digits. */
const isPostalCodeFormatInvalid = computed(() => {
  const postalCode = form.postalCode.trim()
  return postalCode.length > 0 && !POSTAL_CODE_PATTERN.test(postalCode)
})

/** Combines street, house number and the optional supplement into a single "Straße Hausnummer[Zusatz]" line. */
const streetAndHouseNumber = computed(
  () => `${form.street.trim()} ${form.houseNumber.trim()}${form.addressSupplement.trim()}`.trim(),
)

const combinedAddress = computed(() =>
  `${streetAndHouseNumber.value}, ${form.postalCode.trim()} ${form.city.trim()}`.trim(),
)

/** Whether the entered name matches an already-existing property's name, ignoring case and surrounding whitespace. */
const isDuplicateName = computed(() => {
  const name = form.name.trim().toLowerCase()
  if (name.length === 0) return false
  return store.properties.some((property) => property.name.trim().toLowerCase() === name)
})

/** Whether the fully entered address matches an already-existing property's address, ignoring case and surrounding whitespace. */
const isDuplicateAddress = computed(() => {
  if (
    form.street.trim().length === 0 ||
    !STREET_PATTERN.test(form.street.trim()) ||
    !HOUSE_NUMBER_PATTERN.test(form.houseNumber.trim()) ||
    !POSTAL_CODE_PATTERN.test(form.postalCode) ||
    form.city.trim().length === 0
  ) {
    return false
  }
  const address = combinedAddress.value.toLowerCase()
  return store.properties.some((property) => property.address.trim().toLowerCase() === address)
})

watch(visible, (isVisible) => {
  if (isVisible) resetForm()
})

watch(
  () => [form.street, form.houseNumber, form.addressSupplement, form.postalCode, form.city],
  () => {
    validationRequestId += 1
    addressValidation.value = null
    isValidatingAddress.value = false
    scheduleGeocode()
  },
)

/** Resets every field and the location preview to its default, called each time the dialog is opened. */
function resetForm() {
  form.name = ''
  form.street = ''
  form.houseNumber = ''
  form.addressSupplement = ''
  form.postalCode = ''
  form.city = ''
  geocodedPosition.value = null
  isGeocoding.value = false
  addressValidation.value = null
  isValidatingAddress.value = false
  validationRequestId += 1
  if (geocodeTimeout) clearTimeout(geocodeTimeout)
}

/** Debounces geocoding of the current address so it does not fire on every keystroke. */
function scheduleGeocode() {
  if (geocodeTimeout) clearTimeout(geocodeTimeout)

  if (
    form.street.trim().length === 0 ||
    !STREET_PATTERN.test(form.street.trim()) ||
    !HOUSE_NUMBER_PATTERN.test(form.houseNumber.trim()) ||
    !POSTAL_CODE_PATTERN.test(form.postalCode) ||
    form.city.trim().length === 0
  ) {
    geocodedPosition.value = null
    isGeocoding.value = false
    return
  }

  isGeocoding.value = true
  geocodeTimeout = setTimeout(async () => {
    geocodedPosition.value = await geocodeAddress(combinedAddress.value)
    isGeocoding.value = false
  }, GEOCODE_DEBOUNCE_MS)
}

/** Creates the property from the current form state, then closes the dialog on success. */
async function submit() {
  if (!isValid.value || isValidatingAddress.value) return

  const requestId = (validationRequestId += 1)
  isValidatingAddress.value = true
  const result = await validateAddress(form.street, form.houseNumber, form.postalCode, form.city)

  // An address field changed (or another validation started) while this one was in flight; discard the response.
  if (requestId !== validationRequestId) return

  isValidatingAddress.value = false

  if (result.status !== 'MATCH') {
    addressValidation.value = result
    return
  }

  const created = await store.createProperty({
    name: form.name,
    address: combinedAddress.value,
    latitude: geocodedPosition.value?.lat ?? null,
    longitude: geocodedPosition.value?.lng ?? null,
  })

  if (created) visible.value = false
}

/** Applies the last validation's suggested address to the form without any further manual input. */
function acceptSuggestion() {
  if (!addressValidation.value || addressValidation.value.status !== 'SUGGESTION') return

  const { suggestedStreet, suggestedHouseNumber, suggestedPostalCode, suggestedCity } = addressValidation.value
  form.street = suggestedStreet ?? form.street
  form.houseNumber = suggestedHouseNumber ?? form.houseNumber
  form.postalCode = suggestedPostalCode ?? form.postalCode
  form.city = suggestedCity ?? form.city
}

/** Closes the dialog without creating a property. */
function cancel() {
  visible.value = false
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="t('properties.create.title')"
    class="property-form-dialog"
  >
    <div class="property-form-dialog__field">
      <label for="property-name">{{ t('properties.create.nameLabel') }}</label>
      <InputText
        id="property-name"
        v-model="form.name"
        :maxlength="NAME_MAX_LENGTH"
        :invalid="isDuplicateName"
        :placeholder="t('properties.create.namePlaceholder')"
      />
      <p v-if="isDuplicateName" class="property-form-dialog__field-error">
        {{ t('properties.create.duplicateNameError') }}
      </p>
    </div>

    <div class="property-form-dialog__grid property-form-dialog__grid--address">
      <div class="property-form-dialog__field">
        <label for="property-street">{{ t('properties.create.streetLabel') }}</label>
        <InputText
          id="property-street"
          v-model="form.street"
          :maxlength="STREET_MAX_LENGTH"
          :invalid="isDuplicateAddress || isStreetFormatInvalid"
          :placeholder="t('properties.create.streetPlaceholder')"
        />
        <p v-if="isStreetFormatInvalid" class="property-form-dialog__field-error">
          {{ t('properties.create.streetFormatError') }}
        </p>
      </div>

      <div class="property-form-dialog__field">
        <label for="property-house-number">{{ t('properties.create.houseNumberLabel') }}</label>
        <InputText
          id="property-house-number"
          v-model="form.houseNumber"
          :maxlength="HOUSE_NUMBER_MAX_LENGTH"
          inputmode="numeric"
          :invalid="isDuplicateAddress || isHouseNumberFormatInvalid"
          :placeholder="t('properties.create.houseNumberPlaceholder')"
        />
        <p v-if="isHouseNumberFormatInvalid" class="property-form-dialog__field-error">
          {{ t('properties.create.houseNumberFormatError') }}
        </p>
      </div>

      <div class="property-form-dialog__field">
        <label for="property-address-supplement">{{ t('properties.create.addressSupplementLabel') }}</label>
        <InputText
          id="property-address-supplement"
          v-model="form.addressSupplement"
          :maxlength="ADDRESS_SUPPLEMENT_MAX_LENGTH"
          :invalid="isDuplicateAddress"
          :placeholder="t('properties.create.addressSupplementPlaceholder')"
        />
      </div>
    </div>

    <div class="property-form-dialog__grid">
      <div class="property-form-dialog__field">
        <label for="property-postal-code">{{ t('properties.create.postalCodeLabel') }}</label>
        <InputText
          id="property-postal-code"
          v-model="form.postalCode"
          maxlength="5"
          inputmode="numeric"
          :invalid="isDuplicateAddress || isPostalCodeFormatInvalid"
          :placeholder="t('properties.create.postalCodePlaceholder')"
        />
        <p v-if="isPostalCodeFormatInvalid" class="property-form-dialog__field-error">
          {{ t('properties.create.postalCodeFormatError') }}
        </p>
      </div>

      <div class="property-form-dialog__field">
        <label for="property-city">{{ t('properties.create.cityLabel') }}</label>
        <InputText
          id="property-city"
          v-model="form.city"
          :invalid="isDuplicateAddress"
          :placeholder="t('properties.create.cityPlaceholder')"
        />
      </div>
    </div>

    <p v-if="isDuplicateAddress" class="property-form-dialog__field-error">
      {{ t('properties.create.duplicateAddressError') }}
    </p>

    <div v-if="addressValidation?.status === 'SUGGESTION'" class="property-form-dialog__address-suggestion">
      <p class="property-form-dialog__field-error">
        {{ t('properties.create.addressSuggestion', { address: suggestedAddressLine }) }}
      </p>
      <button
        type="button"
        class="property-form-dialog__accept-suggestion"
        @click="acceptSuggestion"
      >
        {{ t('properties.create.acceptSuggestion') }}
      </button>
    </div>

    <p v-if="addressValidation?.status === 'NOT_FOUND'" class="property-form-dialog__field-error">
      {{ t('properties.create.addressNotFoundError') }}
    </p>

    <div class="property-form-dialog__map-section">
      <span class="property-form-dialog__map-hint">{{ t('properties.create.mapHint') }}</span>
      <p v-if="!geocodedPosition && !isGeocoding" class="property-form-dialog__map-empty">
        {{ t('properties.create.mapEmpty') }}
      </p>
      <PropertyLocationPreviewMap v-else :position="geocodedPosition" />
    </div>

    <p v-if="store.hasCreateError" class="property-form-dialog__error">
      {{ t('properties.create.error') }}
    </p>

    <div class="property-form-dialog__actions">
      <Button
        :label="t('properties.create.submit')"
        :disabled="!isValid || isValidatingAddress"
        @click="submit"
      />
      <button type="button" class="property-form-dialog__cancel" @click="cancel">
        {{ t('properties.create.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/properties/property-form-dialog.css"></style>
