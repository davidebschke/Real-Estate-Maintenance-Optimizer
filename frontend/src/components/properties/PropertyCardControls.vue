<script setup lang="ts">
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'

defineEmits<{
  'manage-tenants': []
  edit: []
  delete: []
}>()

const { t } = useI18n()
</script>

<template>
  <div class="property-card-controls" role="group" :aria-label="t('properties.controls.label')">
    <Button
      class="property-card-controls__tenants"
      :label="t('properties.controls.manageTenants')"
      icon="pi pi-users"
      severity="secondary"
      outlined
      size="small"
      @click="$emit('manage-tenants')"
    />
    <Button
      class="property-card-controls__edit"
      :label="t('properties.controls.edit')"
      icon="pi pi-pencil"
      severity="secondary"
      outlined
      size="small"
      @click="$emit('edit')"
    />
    <Button
      class="property-card-controls__delete"
      :label="t('properties.controls.delete')"
      icon="pi pi-trash"
      severity="danger"
      outlined
      size="small"
      @click="$emit('delete')"
    />
  </div>
</template>

<style scoped src="@/styles/properties/property-card-controls.css"></style>
