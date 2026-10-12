<script setup lang="ts">
import { computed, watch } from 'vue'
import Drawer from 'primevue/drawer'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import FormerTenantPaymentList from '@/components/payments/FormerTenantPaymentList.vue'
import PaymentUploadButton from '@/components/payments/PaymentUploadButton.vue'
import RentTrackingActivation from '@/components/payments/RentTrackingActivation.vue'
import TenantPaymentTable from '@/components/payments/TenantPaymentTable.vue'
import ApartmentCard from '@/components/tenants/ApartmentCard.vue'
import { usePaymentsStore } from '@/stores/payments'
import { usePropertiesStore } from '@/stores/properties'
import { useTenantsStore } from '@/stores/tenants'
import { useTenantDeleteConfirmation } from '@/composables/useTenantDeleteConfirmation'
import type { Apartment, Tenant } from '@/types/apartment'

const visible = defineModel<boolean>('visible', { required: true })
const props = defineProps<{ propertyId: string | null }>()

const { t } = useI18n()
const propertiesStore = usePropertiesStore()
const tenantsStore = useTenantsStore()
const paymentsStore = usePaymentsStore()
const { confirmDelete } = useTenantDeleteConfirmation()

const property = computed(
  () => propertiesStore.properties.find((candidate) => candidate.id === props.propertyId) ?? null,
)

const apartments = computed(() => (props.propertyId ? (tenantsStore.apartmentsByProperty[props.propertyId] ?? []) : []))

const paymentOverview = computed(() => (props.propertyId ? paymentsStore.overviewsByProperty[props.propertyId] : undefined))
const isPaymentOverviewCurrent = computed(() => paymentOverview.value?.year === paymentsStore.selectedYear)
const paymentTenants = computed(() => (isPaymentOverviewCurrent.value ? (paymentOverview.value?.tenants ?? []) : []))
const formerTenantPayments = computed(() =>
  isPaymentOverviewCurrent.value ? (paymentOverview.value?.formerTenantPayments ?? []) : [],
)
const isTrackingActive = computed(() => Boolean(paymentOverview.value?.trackingStart))

watch(
  () => props.propertyId,
  (propertyId) => {
    if (!propertyId) return
    tenantsStore.fetchApartments(propertyId)
    paymentsStore.fetchYear(propertyId)
  },
  { immediate: true },
)

/** Closes the detail view. */
function close() {
  visible.value = false
}

/** Opens the tenant form for a new tenant together with a new apartment of this property. */
function createTenant() {
  if (props.propertyId) tenantsStore.openCreateDialog(props.propertyId)
}

/** Shows the rent payment table of this property for another year. */
function changePaymentYear(year: number) {
  if (props.propertyId) paymentsStore.changeYear(props.propertyId, year)
}

/** Asks for delete confirmation, then deletes the tenant. */
function requestDelete(apartment: Apartment, tenant: Tenant) {
  confirmDelete(tenant, apartment.tenants.length === 1, () =>
    tenantsStore.deleteTenant(apartment.propertyId, tenant.id),
  )
}
</script>

<template>
  <Drawer v-model:visible="visible" position="right" class="property-detail-drawer">
    <template v-if="property">
      <button type="button" class="property-detail-drawer__back" @click="close">
        {{ t('tenants.detail.back') }}
      </button>

      <div class="property-detail-drawer__heading">
        <i class="pi property-detail-drawer__icon" :class="property.icon" aria-hidden="true"></i>
        <div>
          <h2 class="property-detail-drawer__name">{{ property.name }}</h2>
          <p class="property-detail-drawer__address">{{ property.address }}</p>
        </div>
      </div>

      <section class="property-detail-drawer__section">
        <h3>{{ t('tenants.detail.tenantsHeading') }}</h3>

        <p v-if="tenantsStore.hasLoadError" class="property-detail-drawer__error">
          {{ t('tenants.detail.loadError') }}
        </p>
        <p v-if="tenantsStore.hasDeleteError" class="property-detail-drawer__error">
          {{ t('tenants.detail.deleteError') }}
        </p>
        <p v-else-if="apartments.length === 0 && !tenantsStore.hasLoadError" class="property-detail-drawer__empty">
          {{ t('tenants.detail.empty') }}
        </p>

        <ApartmentCard
          v-for="apartment in apartments"
          :key="apartment.id"
          :apartment="apartment"
          :alert-by-tenant="paymentsStore.alertByTenant"
          @add-tenant="tenantsStore.openAddTenantDialog"
          @edit-tenant="tenantsStore.openEditDialog"
          @delete-tenant="requestDelete"
        />

        <Button
          class="property-detail-drawer__create"
          :label="t('tenants.detail.createButton')"
          icon="pi pi-plus"
          size="small"
          @click="createTenant"
        />
      </section>

      <section class="property-detail-drawer__section">
        <h3>{{ t('payments.table.heading') }}</h3>

        <RentTrackingActivation
          v-if="paymentOverview"
          :property-id="property.id"
          :tracking-start="paymentOverview.trackingStart"
        />

        <PaymentUploadButton v-if="isTrackingActive" :property-id="property.id" />

        <p v-if="paymentsStore.hasYearLoadError" class="property-detail-drawer__error">
          {{ t('payments.table.loadError') }}
        </p>

        <TenantPaymentTable
          v-if="isTrackingActive || !paymentOverview"
          :year="paymentsStore.selectedYear"
          :tenants="paymentTenants"
          :is-loading="paymentsStore.isYearLoading"
          @change-year="changePaymentYear"
        />

        <FormerTenantPaymentList v-if="formerTenantPayments.length > 0" :payments="formerTenantPayments" />
      </section>
    </template>
  </Drawer>
</template>

<style src="@/styles/properties/property-detail-drawer.css"></style>
