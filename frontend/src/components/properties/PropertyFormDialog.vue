<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import PropertyLocationPreviewMap from '@/components/properties/PropertyLocationPreviewMap.vue'
import { usePropertiesStore } from '@/stores/properties'
import { geocodeAddress, type GeocodedPosition } from '@/services/geocodingService'

const NAME_MAX_LENGTH = 50
const STREET_MAX_LENGTH = 100
const HOUSE_NUMBER_MAX_LENGTH = 10
const ADDRESS_SUPPLEMENT_MAX_LENGTH = 10
const GEOCODE_DEBOUNCE_MS = 500
const POSTAL_CODE_PATTERN = /^\d{5}$/

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

const isValid = computed(
  () =>
    form.name.trim().length > 0 &&
    form.name.length <= NAME_MAX_LENGTH &&
    form.street.trim().length > 0 &&
    form.houseNumber.trim().length > 0 &&
    POSTAL_CODE_PATTERN.test(form.postalCode) &&
    form.city.trim().length > 0,
)

/** Combines street, house number and the optional supplement into a single "Straße Hausnummer[Zusatz]" line. */
const streetAndHouseNumber = computed(
  () => `${form.street.trim()} ${form.houseNumber.trim()}${form.addressSupplement.trim()}`.trim(),
)

const combinedAddress = computed(() =>
  `${streetAndHouseNumber.value}, ${form.postalCode.trim()} ${form.city.trim()}`.trim(),
)

watch(visible, (isVisible) => {
  if (isVisible) resetForm()
})

watch(
  () => [form.street, form.houseNumber, form.addressSupplement, form.postalCode, form.city],
  () => scheduleGeocode(),
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
  if (geocodeTimeout) clearTimeout(geocodeTimeout)
}

/** Debounces geocoding of the current address so it does not fire on every keystroke. */
function scheduleGeocode() {
  if (geocodeTimeout) clearTimeout(geocodeTimeout)

  if (
    form.street.trim().length === 0 ||
    form.houseNumber.trim().length === 0 ||
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
  if (!isValid.value) return

  const created = await store.createProperty({
    name: form.name,
    address: combinedAddress.value,
    latitude: geocodedPosition.value?.lat ?? null,
    longitude: geocodedPosition.value?.lng ?? null,
  })

  if (created) visible.value = false
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
        :placeholder="t('properties.create.namePlaceholder')"
      />
    </div>

    <div class="property-form-dialog__grid property-form-dialog__grid--address">
      <div class="property-form-dialog__field">
        <label for="property-street">{{ t('properties.create.streetLabel') }}</label>
        <InputText
          id="property-street"
          v-model="form.street"
          :maxlength="STREET_MAX_LENGTH"
          :placeholder="t('properties.create.streetPlaceholder')"
        />
      </div>

      <div class="property-form-dialog__field">
        <label for="property-house-number">{{ t('properties.create.houseNumberLabel') }}</label>
        <InputText
          id="property-house-number"
          v-model="form.houseNumber"
          :maxlength="HOUSE_NUMBER_MAX_LENGTH"
          :placeholder="t('properties.create.houseNumberPlaceholder')"
        />
      </div>

      <div class="property-form-dialog__field">
        <label for="property-address-supplement">{{ t('properties.create.addressSupplementLabel') }}</label>
        <InputText
          id="property-address-supplement"
          v-model="form.addressSupplement"
          :maxlength="ADDRESS_SUPPLEMENT_MAX_LENGTH"
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
          :placeholder="t('properties.create.postalCodePlaceholder')"
        />
      </div>

      <div class="property-form-dialog__field">
        <label for="property-city">{{ t('properties.create.cityLabel') }}</label>
        <InputText
          id="property-city"
          v-model="form.city"
          :placeholder="t('properties.create.cityPlaceholder')"
        />
      </div>
    </div>

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
      <Button :label="t('properties.create.submit')" :disabled="!isValid" @click="submit" />
      <button type="button" class="property-form-dialog__cancel" @click="cancel">
        {{ t('properties.create.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/properties/property-form-dialog.css"></style>
