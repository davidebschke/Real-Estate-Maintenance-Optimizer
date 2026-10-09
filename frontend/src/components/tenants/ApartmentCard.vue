<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import { useApartmentFormatting } from '@/composables/useApartmentFormatting'
import type { Apartment, Tenant } from '@/types/apartment'

defineProps<{ apartment: Apartment }>()

defineEmits<{
  'add-tenant': [apartment: Apartment]
  'edit-tenant': [apartment: Apartment, tenant: Tenant]
  'delete-tenant': [apartment: Apartment, tenant: Tenant]
}>()

const { t } = useI18n()
const { formatFloor, formatArea, formatMoney } = useApartmentFormatting()

/** Returns the tenant's first and last name as one display name. */
function fullName(tenant: Tenant): string {
  return `${tenant.firstName} ${tenant.lastName}`
}
</script>

<template>
  <article class="apartment-card">
    <header class="apartment-card__header">
      <h4 class="apartment-card__floor">{{ formatFloor(apartment.floor) }}</h4>
      <span class="apartment-card__area">{{ formatArea(apartment.areaSquareMeters) }}</span>
    </header>

    <dl class="apartment-card__figures">
      <div class="apartment-card__figure">
        <dt>{{ t('tenants.apartment.totalRentLabel') }}</dt>
        <dd>{{ formatMoney(apartment.totalRent) }}</dd>
      </div>
      <div class="apartment-card__figure">
        <dt>{{ t('tenants.apartment.coldRentLabel') }}</dt>
        <dd>{{ formatMoney(apartment.coldRent) }}</dd>
      </div>
      <div class="apartment-card__figure">
        <dt>{{ t('tenants.apartment.additionalCostsLabel') }}</dt>
        <dd>{{ formatMoney(apartment.additionalCosts) }}</dd>
      </div>
    </dl>

    <h5 class="apartment-card__tenants-heading">{{ t('tenants.apartment.tenantsHeading') }}</h5>
    <ul class="apartment-card__tenants">
      <li v-for="tenant in apartment.tenants" :key="tenant.id" class="apartment-card__tenant">
        <span class="apartment-card__tenant-name">{{ fullName(tenant) }}</span>
        <button
          type="button"
          class="apartment-card__tenant-edit"
          :aria-label="t('tenants.apartment.editTenantButton', { name: fullName(tenant) })"
          @click="$emit('edit-tenant', apartment, tenant)"
        >
          <i class="pi pi-pencil" aria-hidden="true"></i>
        </button>
        <button
          type="button"
          class="apartment-card__tenant-delete"
          :aria-label="t('tenants.apartment.deleteTenantButton', { name: fullName(tenant) })"
          @click="$emit('delete-tenant', apartment, tenant)"
        >
          <i class="pi pi-trash" aria-hidden="true"></i>
        </button>
      </li>
    </ul>

    <Button
      class="apartment-card__add-tenant"
      :label="t('tenants.apartment.addTenantButton')"
      icon="pi pi-plus"
      severity="secondary"
      size="small"
      @click="$emit('add-tenant', apartment)"
    />
  </article>
</template>

<style scoped src="@/styles/tenants/apartment-card.css"></style>
