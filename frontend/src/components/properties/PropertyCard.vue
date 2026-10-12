<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useCardTilt } from '@/composables/useCardTilt'
import PaymentAlertBadge from '@/components/payments/PaymentAlertBadge.vue'
import PropertyCardControls from '@/components/properties/PropertyCardControls.vue'
import type { PaymentAlert } from '@/types/payment'
import type { Property } from '@/types/property'

withDefaults(
  defineProps<{
    property: Property
    paymentAlert?: PaymentAlert
    isTrackingActive?: boolean
    isTrackingBusy?: boolean
  }>(),
  { paymentAlert: 'NONE', isTrackingActive: false, isTrackingBusy: false },
)

defineEmits<{
  'open-detail': [propertyId: string]
  'add-apartment': [propertyId: string]
  'activate-tracking': [propertyId: string]
  'deactivate-tracking': [propertyId: string]
  'edit-property': [property: Property]
  'delete-property': [property: Property]
}>()

const { t } = useI18n()
const { tiltStyle, onPointerMove, onPointerLeave } = useCardTilt()
</script>

<template>
  <div
    class="property-card card-3d card-3d--interactive"
    :style="tiltStyle"
    @pointermove="onPointerMove"
    @pointerleave="onPointerLeave"
  >
    <div class="property-card__header">
      <i class="pi property-card__icon" :class="property.icon" aria-hidden="true"></i>
      <div class="property-card__heading">
        <h3 class="property-card__name">
          <button
            v-tooltip.top="t('properties.card.detailsButton', { name: property.name })"
            type="button"
            class="property-card__details"
            @click="$emit('open-detail', property.id)"
          >
            {{ property.name }}
          </button>
        </h3>
        <p class="property-card__address">{{ property.address }}</p>
        <PaymentAlertBadge class="property-card__payment-alert" :alert="paymentAlert" />
      </div>
    </div>

    <div class="property-card__stats">
      <div class="property-card__stat">
        <span class="property-card__stat-value">{{ property.tenantCount }}</span>
        <span class="property-card__stat-label">{{ t('properties.card.tenantCount') }}</span>
      </div>
    </div>

    <PropertyCardControls
      :is-tracking-active="isTrackingActive"
      :is-tracking-busy="isTrackingBusy"
      @manage-tenants="$emit('open-detail', property.id)"
      @add-apartment="$emit('add-apartment', property.id)"
      @activate-tracking="$emit('activate-tracking', property.id)"
      @deactivate-tracking="$emit('deactivate-tracking', property.id)"
      @edit="$emit('edit-property', property)"
      @delete="$emit('delete-property', property)"
    />
  </div>
</template>

<style scoped src="@/styles/card-3d.css"></style>
<style scoped src="@/styles/properties/property-card.css"></style>
