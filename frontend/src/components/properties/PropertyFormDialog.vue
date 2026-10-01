<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import PropertyLocationPreviewMap from '@/components/properties/PropertyLocationPreviewMap.vue'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import DemoQuotaHint from '@/components/auth/DemoQuotaHint.vue'
import { usePropertiesStore } from '@/stores/properties'
import { useDemoQuota } from '@/composables/useDemoQuota'
import {
  geocodeAddress,
  validateAddress,
  type AddressValidationResult,
  type GeocodedPosition,
} from '@/services/geocodingService'
import { parsePropertyAddress } from '@/utils/propertyAddressParsing'

const NAME_MAX_LENGTH = 50
const STREET_MAX_LENGTH = 100
const HOUSE_NUMBER_MAX_LENGTH = 5
const ADDRESS_SUPPLEMENT_MAX_LENGTH = 10
const CITY_MAX_LENGTH = 50
const GEOCODE_DEBOUNCE_MS = 500
const HOUSE_NUMBER_PATTERN = /^\d{1,5}$/
const POSTAL_CODE_PATTERN = /^\d{5}$/
/** Letters (incl. diacritics), digits, spaces, punctuation common to real German street/place names, and the typographic apostrophe/dash/non-breaking-space "smart punctuation" autocorrect commonly substitutes. */
const PLACE_NAME_PATTERN = /^[\p{L}\d .'’ /–-]+$/u

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

/** Whether each required field has already been left (blurred) at least once, so its required-field hint may be shown. */
const touched = reactive({
  name: false,
  street: false,
  houseNumber: false,
  postalCode: false,
  city: false,
})

/** Whether the dialog is currently editing an existing property rather than creating a new one. */
const isEditMode = computed(() => store.editingProperty !== null)

const { isExhausted: isCreationQuotaExhausted } = useDemoQuota('properties')

/** Whether a demo account already used up its property creations, which blocks creating but never editing. */
const isBlockedByQuota = computed(() => !isEditMode.value && isCreationQuotaExhausted.value)

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

/** Whether street, house number, postal code and city are all present and match their respective format. */
const isAddressFormatValid = computed(
  () =>
    form.street.trim().length > 0 &&
    PLACE_NAME_PATTERN.test(form.street.trim()) &&
    HOUSE_NUMBER_PATTERN.test(form.houseNumber.trim()) &&
    POSTAL_CODE_PATTERN.test(form.postalCode) &&
    form.city.trim().length > 0 &&
    PLACE_NAME_PATTERN.test(form.city.trim()),
)

const isValid = computed(
  () =>
    form.name.trim().length > 0 &&
    form.name.length <= NAME_MAX_LENGTH &&
    isAddressFormatValid.value &&
    !isDuplicateName.value &&
    !isDuplicateAddress.value &&
    !hasBlockingAddressError.value,
)

/** Whether the street has been touched but contains a character that occurs in no real German street name. */
const isStreetFormatInvalid = computed(() => {
  const street = form.street.trim()
  return street.length > 0 && !PLACE_NAME_PATTERN.test(street)
})

/** Whether the city has been touched but contains a character that occurs in no real German place name. */
const isCityFormatInvalid = computed(() => {
  const city = form.city.trim()
  return city.length > 0 && !PLACE_NAME_PATTERN.test(city)
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

/** Whether the name has been left empty after being touched, i.e. its required-field hint must be shown. */
const isNameMissing = computed(() => touched.name && form.name.trim().length === 0)

/** Whether the street has been left empty after being touched, i.e. its required-field hint must be shown. */
const isStreetMissing = computed(() => touched.street && form.street.trim().length === 0)

/** Whether the house number has been left empty after being touched, i.e. its required-field hint must be shown. */
const isHouseNumberMissing = computed(() => touched.houseNumber && form.houseNumber.trim().length === 0)

/** Whether the postal code has been left empty after being touched, i.e. its required-field hint must be shown. */
const isPostalCodeMissing = computed(() => touched.postalCode && form.postalCode.trim().length === 0)

/** Whether the city has been left empty after being touched, i.e. its required-field hint must be shown. */
const isCityMissing = computed(() => touched.city && form.city.trim().length === 0)

/** Combines street, house number and the optional supplement into a single "Straße Hausnummer[Zusatz]" line. */
const streetAndHouseNumber = computed(
  () => `${form.street.trim()} ${form.houseNumber.trim()}${form.addressSupplement.trim()}`.trim(),
)

const combinedAddress = computed(() =>
  `${streetAndHouseNumber.value}, ${form.postalCode.trim()} ${form.city.trim()}`.trim(),
)

/** Every property other than the one currently being edited, i.e. the set a duplicate name/address is checked against. */
const otherProperties = computed(() =>
  store.properties.filter((property) => property.id !== store.editingProperty?.id),
)

/** Whether the entered name matches an already-existing property's name, ignoring case and surrounding whitespace. */
const isDuplicateName = computed(() => {
  const name = form.name.trim().toLowerCase()
  if (name.length === 0) return false
  return otherProperties.value.some((property) => property.name.trim().toLowerCase() === name)
})

/** Whether the fully entered address matches an already-existing property's address, ignoring case and surrounding whitespace. */
const isDuplicateAddress = computed(() => {
  if (!isAddressFormatValid.value) {
    return false
  }
  const address = combinedAddress.value.toLowerCase()
  return otherProperties.value.some((property) => property.address.trim().toLowerCase() === address)
})

watch(visible, (isVisible) => {
  if (isVisible) resetForm()
})

watch(
  () => [form.street, form.houseNumber, form.postalCode, form.city],
  () => {
    validationRequestId += 1
    addressValidation.value = null
    isValidatingAddress.value = false
  },
)

watch(
  () => [form.street, form.houseNumber, form.addressSupplement, form.postalCode, form.city],
  () => scheduleGeocode(),
)

/** Fills every field from the property being edited, falling back to blank fields if its address cannot be parsed. */
function fillFormFromEditingProperty(property: NonNullable<typeof store.editingProperty>) {
  const parsedAddress = parsePropertyAddress(property.address)
  form.name = property.name
  form.street = parsedAddress?.street ?? ''
  form.houseNumber = parsedAddress?.houseNumber ?? ''
  form.addressSupplement = parsedAddress?.addressSupplement ?? ''
  form.postalCode = parsedAddress?.postalCode ?? ''
  form.city = parsedAddress?.city ?? ''
  geocodedPosition.value =
    property.latitude !== null && property.longitude !== null
      ? { lat: property.latitude, lng: property.longitude }
      : null
}

/** Resets every field and the location preview to its default, called each time the dialog is opened. */
function resetForm() {
  if (store.editingProperty) {
    fillFormFromEditingProperty(store.editingProperty)
  } else {
    form.name = ''
    form.street = ''
    form.houseNumber = ''
    form.addressSupplement = ''
    form.postalCode = ''
    form.city = ''
    geocodedPosition.value = null
  }
  isGeocoding.value = false
  addressValidation.value = null
  isValidatingAddress.value = false
  validationRequestId += 1
  if (geocodeTimeout) clearTimeout(geocodeTimeout)
  touched.name = false
  touched.street = false
  touched.houseNumber = false
  touched.postalCode = false
  touched.city = false
}

/** Debounces geocoding of the current address so it does not fire on every keystroke. */
function scheduleGeocode() {
  if (geocodeTimeout) clearTimeout(geocodeTimeout)

  if (!isAddressFormatValid.value) {
    geocodedPosition.value = null
    isGeocoding.value = false
    return
  }

  // the address still matches the property being edited unchanged, so its already-seeded, stored coordinates must not be overwritten by a redundant free-text re-geocode
  if (store.editingProperty && combinedAddress.value === store.editingProperty.address) {
    isGeocoding.value = false
    return
  }

  isGeocoding.value = true
  geocodeTimeout = setTimeout(async () => {
    geocodedPosition.value = await geocodeAddress(combinedAddress.value)
    isGeocoding.value = false
  }, GEOCODE_DEBOUNCE_MS)
}

/** Whether an address field changed, or another validation started, since the given validation request was sent. */
function isStaleValidationResponse(requestId: number) {
  return requestId !== validationRequestId
}

/** Creates the property from the current form state, then closes the dialog on success. */
async function submit() {
  if (!isValid.value || isValidatingAddress.value || isBlockedByQuota.value) return

  const requestId = (validationRequestId += 1)
  isValidatingAddress.value = true
  const result = await validateAddress(form.street, form.houseNumber, form.postalCode, form.city)

  if (isStaleValidationResponse(requestId)) return

  isValidatingAddress.value = false

  if (result.status !== 'MATCH') {
    addressValidation.value = result
    return
  }

  if (!isValid.value) return

  const payload = {
    name: form.name,
    address: combinedAddress.value,
    latitude: result.latitude ?? geocodedPosition.value?.lat ?? null,
    longitude: result.longitude ?? geocodedPosition.value?.lng ?? null,
  }

  const succeeded = store.editingProperty
    ? await store.updateProperty(store.editingProperty.id, payload)
    : await store.createProperty(payload)

  if (succeeded) visible.value = false
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
    :header="isEditMode ? t('properties.edit.title') : t('properties.create.title')"
    class="property-form-dialog"
  >
    <div class="property-form-dialog__field">
      <RequiredFieldLabel field-id="property-name" :label="t('properties.create.nameLabel')" />
      <InputText
        id="property-name"
        v-model="form.name"
        :maxlength="NAME_MAX_LENGTH"
        :invalid="isDuplicateName || isNameMissing"
        aria-required="true"
        :aria-describedby="isNameMissing || isDuplicateName ? 'property-name-error' : undefined"
        :placeholder="t('properties.create.namePlaceholder')"
        @blur="touched.name = true"
      />
      <p v-if="isNameMissing" id="property-name-error" class="property-form-dialog__field-error">
        {{ t('properties.create.nameRequiredError') }}
      </p>
      <p v-else-if="isDuplicateName" id="property-name-error" class="property-form-dialog__field-error">
        {{ t('properties.create.duplicateNameError') }}
      </p>
    </div>

    <div class="property-form-dialog__grid property-form-dialog__grid--address">
      <div class="property-form-dialog__field">
        <RequiredFieldLabel field-id="property-street" :label="t('properties.create.streetLabel')" />
        <InputText
          id="property-street"
          v-model="form.street"
          :maxlength="STREET_MAX_LENGTH"
          :invalid="isDuplicateAddress || isStreetFormatInvalid || isStreetMissing"
          aria-required="true"
          :aria-describedby="isStreetMissing || isStreetFormatInvalid ? 'property-street-error' : undefined"
          :placeholder="t('properties.create.streetPlaceholder')"
          @blur="touched.street = true"
        />
        <p v-if="isStreetMissing" id="property-street-error" class="property-form-dialog__field-error">
          {{ t('properties.create.streetRequiredError') }}
        </p>
        <p v-else-if="isStreetFormatInvalid" id="property-street-error" class="property-form-dialog__field-error">
          {{ t('properties.create.streetFormatError') }}
        </p>
      </div>

      <div class="property-form-dialog__field">
        <RequiredFieldLabel field-id="property-house-number" :label="t('properties.create.houseNumberLabel')" />
        <InputText
          id="property-house-number"
          v-model="form.houseNumber"
          :maxlength="HOUSE_NUMBER_MAX_LENGTH"
          inputmode="numeric"
          :invalid="isDuplicateAddress || isHouseNumberFormatInvalid || isHouseNumberMissing"
          aria-required="true"
          :aria-describedby="
            isHouseNumberMissing || isHouseNumberFormatInvalid ? 'property-house-number-error' : undefined
          "
          :placeholder="t('properties.create.houseNumberPlaceholder')"
          @blur="touched.houseNumber = true"
        />
        <p v-if="isHouseNumberMissing" id="property-house-number-error" class="property-form-dialog__field-error">
          {{ t('properties.create.houseNumberRequiredError') }}
        </p>
        <p
          v-else-if="isHouseNumberFormatInvalid"
          id="property-house-number-error"
          class="property-form-dialog__field-error"
        >
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
        <RequiredFieldLabel field-id="property-postal-code" :label="t('properties.create.postalCodeLabel')" />
        <InputText
          id="property-postal-code"
          v-model="form.postalCode"
          maxlength="5"
          inputmode="numeric"
          :invalid="isDuplicateAddress || isPostalCodeFormatInvalid || isPostalCodeMissing"
          aria-required="true"
          :aria-describedby="
            isPostalCodeMissing || isPostalCodeFormatInvalid ? 'property-postal-code-error' : undefined
          "
          :placeholder="t('properties.create.postalCodePlaceholder')"
          @blur="touched.postalCode = true"
        />
        <p v-if="isPostalCodeMissing" id="property-postal-code-error" class="property-form-dialog__field-error">
          {{ t('properties.create.postalCodeRequiredError') }}
        </p>
        <p
          v-else-if="isPostalCodeFormatInvalid"
          id="property-postal-code-error"
          class="property-form-dialog__field-error"
        >
          {{ t('properties.create.postalCodeFormatError') }}
        </p>
      </div>

      <div class="property-form-dialog__field">
        <RequiredFieldLabel field-id="property-city" :label="t('properties.create.cityLabel')" />
        <InputText
          id="property-city"
          v-model="form.city"
          :maxlength="CITY_MAX_LENGTH"
          :invalid="isDuplicateAddress || isCityFormatInvalid || isCityMissing"
          aria-required="true"
          :aria-describedby="isCityMissing || isCityFormatInvalid ? 'property-city-error' : undefined"
          :placeholder="t('properties.create.cityPlaceholder')"
          @blur="touched.city = true"
        />
        <p v-if="isCityMissing" id="property-city-error" class="property-form-dialog__field-error">
          {{ t('properties.create.cityRequiredError') }}
        </p>
        <p v-else-if="isCityFormatInvalid" id="property-city-error" class="property-form-dialog__field-error">
          {{ t('properties.create.cityFormatError') }}
        </p>
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

    <p v-if="isEditMode ? store.hasUpdateError : store.hasCreateError" class="property-form-dialog__error">
      {{ isEditMode ? t('properties.edit.error') : t('properties.create.error') }}
    </p>

    <DemoQuotaHint v-if="!isEditMode" resource="properties" />

    <p class="property-form-dialog__required-legend">{{ t('properties.create.requiredFieldsLegend') }}</p>

    <div class="property-form-dialog__actions">
      <Button
        :label="isEditMode ? t('properties.edit.submit') : t('properties.create.submit')"
        :disabled="!isValid || isValidatingAddress || isBlockedByQuota"
        @click="submit"
      />
      <button type="button" class="property-form-dialog__cancel" @click="cancel">
        {{ t('properties.create.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/properties/property-form-dialog.css"></style>
