<script setup lang="ts">
import { computed, onMounted } from 'vue'
import Select from 'primevue/select'
import { useI18n } from 'vue-i18n'
import { usePropertiesStore } from '@/stores/properties'

const modelValue = defineModel<string | null>({ required: true })

withDefaults(defineProps<{ required?: boolean; invalid?: boolean }>(), {
  required: false,
  invalid: false,
})

const { t } = useI18n()
const propertiesStore = usePropertiesStore()

const selectedProperty = computed(
  () => propertiesStore.properties.find((property) => property.id === modelValue.value) ?? null,
)

onMounted(() => {
  propertiesStore.fetchProperties()
})
</script>

<template>
  <div class="appointment-property-select">
    <label class="appointment-property-select__label" for="appointment-property">
      {{ t('appointments.form.property.label')
      }}<span v-if="required" class="appointment-property-select__required-marker" aria-hidden="true"> *</span>
    </label>
    <Select
      input-id="appointment-property"
      v-model="modelValue"
      class="appointment-property-select__input"
      :options="propertiesStore.properties"
      option-label="name"
      option-value="id"
      :invalid="invalid"
      :placeholder="t('appointments.form.property.placeholder')"
    />
    <p v-if="invalid" class="appointment-property-select__error">
      {{ t('appointments.form.property.requiredError') }}
    </p>
    <p v-if="selectedProperty" class="appointment-property-select__address">
      {{ selectedProperty.address }} — {{ t('appointments.form.property.sourceHint') }}
    </p>
  </div>
</template>

<style scoped src="@/styles/appointments/appointment-property-select.css"></style>
