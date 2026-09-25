<script setup lang="ts">
import { computed } from 'vue'
import { RouterView } from 'vue-router'
import ConfirmDialog from 'primevue/confirmdialog'
import Toast from 'primevue/toast'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import AppointmentFormDialog from '@/components/appointments/AppointmentFormDialog.vue'
import AppointmentDetailDrawer from '@/components/appointments/AppointmentDetailDrawer.vue'
import PropertyFormDialog from '@/components/properties/PropertyFormDialog.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertiesStore } from '@/stores/properties'

const appointmentsStore = useAppointmentsStore()
const propertiesStore = usePropertiesStore()

/** Whether the detail drawer is open, derived from which appointment (if any) is active. */
const isDetailVisible = computed({
  get: () => appointmentsStore.activeDetailAppointmentId !== null,
  set: (value: boolean) => {
    if (!value) appointmentsStore.closeDetail()
  },
})
</script>

<template>
  <div class="app-layout">
    <AppHeader />
    <main class="app-content">
      <RouterView />
    </main>
    <AppFooter />

    <AppointmentFormDialog v-model:visible="appointmentsStore.isCreateDialogOpen" />
    <AppointmentDetailDrawer
      v-model:visible="isDetailVisible"
      :appointment-id="appointmentsStore.activeDetailAppointmentId"
    />
    <PropertyFormDialog v-model:visible="propertiesStore.isCreateDialogOpen" />
    <ConfirmDialog />
    <Toast />
  </div>
</template>

<style scoped src="@/styles/app.css"></style>
