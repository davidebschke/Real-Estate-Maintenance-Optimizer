<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{ isTrackingActive?: boolean; isTrackingBusy?: boolean }>(), {
  isTrackingActive: false,
  isTrackingBusy: false,
})

const emit = defineEmits<{
  'manage-tenants': []
  'add-apartment': []
  'activate-tracking': []
  'deactivate-tracking': []
  edit: []
  delete: []
}>()

const { t } = useI18n()

const trackingLabel = computed(() =>
  t(props.isTrackingActive ? 'properties.controls.deactivateTracking' : 'properties.controls.activateTracking'),
)

/** Asks to deactivate the rent payment check while it is active, otherwise to activate it. */
function toggleTracking() {
  if (props.isTrackingActive) emit('deactivate-tracking')
  else emit('activate-tracking')
}
</script>

<template>
  <div class="property-card-controls" role="group" :aria-label="t('properties.controls.label')">
    <Button
      class="property-card-controls__tenants"
      :label="t('properties.controls.manageTenants')"
      :aria-label="t('properties.controls.manageTenants')"
      icon="pi pi-users"
      severity="secondary"
      outlined
      size="small"
      @click="$emit('manage-tenants')"
    />
    <Button
      class="property-card-controls__apartment"
      :label="t('properties.controls.addApartment')"
      :aria-label="t('properties.controls.addApartment')"
      icon="pi pi-plus"
      severity="secondary"
      outlined
      size="small"
      @click="$emit('add-apartment')"
    />
    <Button
      class="property-card-controls__tracking"
      :label="trackingLabel"
      :aria-label="trackingLabel"
      icon="pi pi-power-off"
      severity="secondary"
      outlined
      size="small"
      :loading="isTrackingBusy"
      :disabled="isTrackingBusy"
      @click="toggleTracking"
    />
    <Button
      class="property-card-controls__edit"
      :label="t('properties.controls.edit')"
      :aria-label="t('properties.controls.edit')"
      icon="pi pi-pencil"
      severity="secondary"
      outlined
      size="small"
      @click="$emit('edit')"
    />
    <Button
      class="property-card-controls__delete"
      :label="t('properties.controls.delete')"
      :aria-label="t('properties.controls.delete')"
      icon="pi pi-trash"
      severity="danger"
      outlined
      size="small"
      @click="$emit('delete')"
    />
  </div>
</template>

<style scoped src="@/styles/properties/property-card-controls.css"></style>
