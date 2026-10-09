<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import ConfirmDialog from 'primevue/confirmdialog'
import Toast from 'primevue/toast'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import AppointmentFormDialog from '@/components/appointments/AppointmentFormDialog.vue'
import AppointmentDetailDrawer from '@/components/appointments/AppointmentDetailDrawer.vue'
import PropertyFormDialog from '@/components/properties/PropertyFormDialog.vue'
import PropertyDetailDrawer from '@/components/properties/PropertyDetailDrawer.vue'
import TenantFormDialog from '@/components/tenants/TenantFormDialog.vue'
import DemoAccountBanner from '@/components/auth/DemoAccountBanner.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertiesStore } from '@/stores/properties'
import { useTenantsStore } from '@/stores/tenants'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const appointmentsStore = useAppointmentsStore()
const propertiesStore = usePropertiesStore()
const tenantsStore = useTenantsStore()
const authStore = useAuthStore()

/** Whether the full app shell is shown; the login screen renders on its own instead, and a protected page without session renders nothing until the guard redirected to the login screen. */
const showsAppShell = computed(() => authStore.isAuthenticated && route.meta.layout !== 'auth')

/** Whether the detail drawer is open, derived from which appointment (if any) is active. */
const isDetailVisible = computed({
  get: () => appointmentsStore.activeDetailAppointmentId !== null,
  set: (value: boolean) => {
    if (!value) appointmentsStore.closeDetail()
  },
})

/** Whether the property detail drawer is open, derived from which property (if any) is active. */
const isPropertyDetailVisible = computed({
  get: () => propertiesStore.activeDetailPropertyId !== null,
  set: (value: boolean) => {
    if (!value) propertiesStore.closeDetail()
  },
})
</script>

<template>
  <div v-if="showsAppShell" class="app-layout">
    <AppHeader />
    <DemoAccountBanner />
    <main class="app-content">
      <RouterView />
    </main>
    <AppFooter />

    <AppointmentFormDialog v-model:visible="appointmentsStore.isFormDialogOpen" />
    <AppointmentDetailDrawer
      v-model:visible="isDetailVisible"
      :appointment-id="appointmentsStore.activeDetailAppointmentId"
    />
    <PropertyFormDialog v-model:visible="propertiesStore.isFormDialogOpen" />
    <PropertyDetailDrawer
      v-model:visible="isPropertyDetailVisible"
      :property-id="propertiesStore.activeDetailPropertyId"
    />
    <TenantFormDialog v-model:visible="tenantsStore.isFormDialogOpen" />
    <ConfirmDialog />
    <Toast />
  </div>
  <RouterView v-else-if="route.meta.public" />
</template>

<style scoped src="@/styles/app.css"></style>
<style src="@/styles/confirm-dialog.css"></style>
