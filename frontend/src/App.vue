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
import DemoAccountBanner from '@/components/auth/DemoAccountBanner.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertiesStore } from '@/stores/properties'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const appointmentsStore = useAppointmentsStore()
const propertiesStore = usePropertiesStore()
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
</script>

<template>
  <div v-if="showsAppShell" class="app-layout">
    <AppHeader />
    <DemoAccountBanner />
    <main class="app-content">
      <RouterView />
    </main>
    <AppFooter />

    <AppointmentFormDialog v-model:visible="appointmentsStore.isCreateDialogOpen" />
    <AppointmentDetailDrawer
      v-model:visible="isDetailVisible"
      :appointment-id="appointmentsStore.activeDetailAppointmentId"
    />
    <PropertyFormDialog v-model:visible="propertiesStore.isFormDialogOpen" />
    <ConfirmDialog />
    <Toast />
  </div>
  <RouterView v-else-if="route.meta.public" />
</template>

<style scoped src="@/styles/app.css"></style>
<style src="@/styles/confirm-dialog.css"></style>
