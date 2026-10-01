<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Select from 'primevue/select'
import { useI18n } from 'vue-i18n'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import { usePropertiesStore } from '@/stores/properties'

const modelValue = defineModel<string | null>({ required: true })

const props = withDefaults(defineProps<{ required?: boolean }>(), {
  required: false,
})

const { t } = useI18n()
const propertiesStore = usePropertiesStore()

/** Whether the select has already been left (blurred) at least once, so its required-field hint may be shown. */
const isTouched = ref(false)

/** Whether no property has been left chosen after the select was touched, i.e. its required-field hint must be shown. */
const isMissing = computed(() => props.required && isTouched.value && modelValue.value === null)

const selectedProperty = computed(
  () => propertiesStore.properties.find((property) => property.id === modelValue.value) ?? null,
)

onMounted(() => {
  propertiesStore.fetchProperties()
})
</script>

<template>
  <div class="appointment-property-select">
    <RequiredFieldLabel
      class="appointment-property-select__label"
      field-id="appointment-property"
      :label="t('appointments.form.property.label')"
      :required="required"
    />
    <Select
      input-id="appointment-property"
      v-model="modelValue"
      class="appointment-property-select__input"
      :options="propertiesStore.properties"
      option-label="name"
      option-value="id"
      :invalid="isMissing"
      :aria-required="required"
      :aria-describedby="isMissing ? 'appointment-property-error' : undefined"
      :placeholder="t('appointments.form.property.placeholder')"
      @blur="isTouched = true"
    />
    <p v-if="isMissing" id="appointment-property-error" class="appointment-property-select__error">
      {{ t('appointments.form.property.requiredError') }}
    </p>
    <p v-if="selectedProperty" class="appointment-property-select__address">
      {{ selectedProperty.address }} — {{ t('appointments.form.property.sourceHint') }}
    </p>
  </div>
</template>

<style scoped src="@/styles/appointments/appointment-property-select.css"></style>
