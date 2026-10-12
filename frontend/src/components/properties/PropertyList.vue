<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePaymentErrorText } from '@/composables/usePaymentErrorText'
import { usePaymentsStore } from '@/stores/payments'
import { usePropertiesStore } from '@/stores/properties'
import { useTenantsStore } from '@/stores/tenants'
import { usePropertyDeleteConfirmation } from '@/composables/usePropertyDeleteConfirmation'
import { useTrackingDeactivationConfirmation } from '@/composables/useTrackingDeactivationConfirmation'
import PropertyCard from '@/components/properties/PropertyCard.vue'
import PropertyCreateCard from '@/components/properties/PropertyCreateCard.vue'
import type { Property } from '@/types/property'

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const paymentsStore = usePaymentsStore()
const tenantsStore = useTenantsStore()
const { confirmDelete } = usePropertyDeleteConfirmation()
const { confirmDeactivation } = useTrackingDeactivationConfirmation()
const { describeError } = usePaymentErrorText()

const busyTrackingPropertyId = ref<string | null>(null)

const activationErrorText = computed(() =>
  paymentsStore.activationError ? describeError(paymentsStore.activationError, 'payments.tracking.error') : null,
)
const deactivationErrorText = computed(() =>
  paymentsStore.deactivationError
    ? describeError(paymentsStore.deactivationError, 'payments.tracking.deactivateError')
    : null,
)

onMounted(() => {
  propertiesStore.fetchProperties()
  paymentsStore.fetchAlerts()
})

function handleDeleteProperty(property: Property) {
  confirmDelete(property, () => propertiesStore.deleteProperty(property.id))
}

/** Returns whether the rent payment check of the property is active, preferring its freshly loaded payment table over the start month the property came with. */
function isTrackingActive(property: Property): boolean {
  const overview = paymentsStore.overviewsByProperty[property.id]
  return Boolean(overview ? overview.trackingStart : property.rentTrackingStart)
}

/** Runs a change of a property's rent payment check, remembering which card shows the loading state. */
async function runTrackingChange(propertyId: string, change: () => Promise<boolean>) {
  paymentsStore.clearActivationError()
  paymentsStore.clearDeactivationError()
  busyTrackingPropertyId.value = propertyId
  try {
    await change()
  } finally {
    busyTrackingPropertyId.value = null
  }
}

function handleActivateTracking(propertyId: string) {
  return runTrackingChange(propertyId, () => paymentsStore.activateTracking(propertyId))
}

function handleDeactivateTracking(propertyId: string) {
  confirmDeactivation(() => runTrackingChange(propertyId, () => paymentsStore.deactivateTracking(propertyId)))
}

onBeforeUnmount(() => {
  paymentsStore.clearActivationError()
  paymentsStore.clearDeactivationError()
})
</script>

<template>
  <div class="property-list">
    <PropertyCreateCard />

    <p v-if="propertiesStore.hasDeleteError" class="property-list__error">
      {{ t('properties.list.deleteError') }}
    </p>
    <p v-if="paymentsStore.hasAlertsLoadError" class="property-list__error">
      {{ t('payments.alerts.loadError') }}
    </p>
    <p v-if="activationErrorText" class="property-list__error" role="alert">{{ activationErrorText }}</p>
    <p v-if="deactivationErrorText" class="property-list__error" role="alert">{{ deactivationErrorText }}</p>
    <p v-if="propertiesStore.hasLoadError" class="property-list__error">
      {{ t('properties.list.loadError') }}
    </p>
    <p
      v-else-if="propertiesStore.properties.length === 0"
      class="property-list__empty"
    >
      {{ t('properties.list.empty') }}
    </p>
    <PropertyCard
      v-for="property in propertiesStore.properties"
      :key="property.id"
      :property="property"
      :payment-alert="paymentsStore.alertForProperty(property.id)"
      :is-tracking-active="isTrackingActive(property)"
      :is-tracking-busy="busyTrackingPropertyId === property.id"
      @open-detail="propertiesStore.openDetail"
      @add-apartment="tenantsStore.openCreateApartmentDialog"
      @activate-tracking="handleActivateTracking"
      @deactivate-tracking="handleDeactivateTracking"
      @edit-property="propertiesStore.openEditDialog"
      @delete-property="handleDeleteProperty"
    />
  </div>
</template>

<style scoped src="@/styles/properties/property-list.css"></style>
