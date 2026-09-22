<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useCardTilt } from '@/composables/useCardTilt'
import { usePropertiesStore } from '@/stores/properties'

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const { tiltStyle, onPointerMove, onPointerLeave } = useCardTilt()
</script>

<template>
  <button
    type="button"
    class="property-create-card card-3d card-3d--interactive"
    :style="tiltStyle"
    :aria-label="t('properties.create.cardLabel')"
    @pointermove="onPointerMove"
    @pointerleave="onPointerLeave"
    @click="propertiesStore.openCreateDialog()"
  >
    <i class="pi pi-plus property-create-card__icon" aria-hidden="true"></i>
    <span class="property-create-card__label">{{ t('properties.create.cardLabel') }}</span>
  </button>
</template>

<style scoped src="@/styles/card-3d.css"></style>
<style scoped src="@/styles/properties/property-create-card.css"></style>
