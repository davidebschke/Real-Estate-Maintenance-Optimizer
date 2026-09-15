<script setup lang="ts">
import { computed } from 'vue'
import Select from 'primevue/select'
import { useI18n } from 'vue-i18n'
import { exampleProperties } from '@/data/exampleProperties'

const modelValue = defineModel<string | null>({ required: true })

const { t } = useI18n()

const selectedProperty = computed(
  () => exampleProperties.find((property) => property.id === modelValue.value) ?? null,
)
</script>

<template>
  <div class="appointment-property-select">
    <label class="appointment-property-select__label" for="appointment-property">
      {{ t('appointments.form.property.label') }}
    </label>
    <Select
      input-id="appointment-property"
      v-model="modelValue"
      class="appointment-property-select__input"
      :options="exampleProperties"
      option-label="name"
      option-value="id"
      :placeholder="t('appointments.form.property.placeholder')"
    />
    <p v-if="selectedProperty" class="appointment-property-select__address">
      {{ selectedProperty.address }} — {{ t('appointments.form.property.sourceHint') }}
    </p>
  </div>
</template>

<style scoped src="@/styles/appointments/appointment-property-select.css"></style>
