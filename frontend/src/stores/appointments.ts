import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as appointmentService from '@/services/appointmentService'
import type {
  AppointmentDeleteScope,
  CreateAppointmentPayload,
  MoveAppointmentPayload,
} from '@/services/appointmentService'
import type { Appointment } from '@/types/appointment'

/** Holds every appointment and the create/detail overlay state shared across the app. */
export const useAppointmentsStore = defineStore('appointments', () => {
  const appointments = ref<Appointment[]>([])
  const isCreateDialogOpen = ref(false)
  const activeDetailAppointmentId = ref<string | null>(null)

  /** Loads every appointment from the backend. */
  async function fetchAppointments() {
    appointments.value = await appointmentService.fetchAppointments()
  }

  /** Creates a new appointment and adds its returned (first) occurrence to the local list. */
  async function createAppointment(payload: CreateAppointmentPayload) {
    const created = await appointmentService.createAppointment(payload)
    await fetchAppointments()
    return created
  }

  /** Reschedules an appointment and refreshes the local list from the backend. */
  async function moveAppointment(id: string, payload: MoveAppointmentPayload) {
    const moved = await appointmentService.moveAppointment(id, payload)
    await fetchAppointments()
    return moved
  }

  /** Marks an appointment as completed with the current time as its actual end, and refreshes the local list. */
  async function completeAppointment(id: string) {
    const completed = await appointmentService.completeAppointment(id)
    await fetchAppointments()
    return completed
  }

  /** Reverts a completed appointment back to its not-yet-completed state, and refreshes the local list. */
  async function reopenAppointment(id: string) {
    const reopened = await appointmentService.reopenAppointment(id)
    await fetchAppointments()
    return reopened
  }

  /** Deletes an appointment (optionally its whole recurring series) and refreshes the local list. */
  async function deleteAppointment(id: string, scope: AppointmentDeleteScope = 'single') {
    await appointmentService.deleteAppointment(id, scope)
    await fetchAppointments()
  }

  /** Opens the appointment creation form. */
  function openCreateDialog() {
    isCreateDialogOpen.value = true
  }

  /** Closes the appointment creation form. */
  function closeCreateDialog() {
    isCreateDialogOpen.value = false
  }

  /** Opens the detail view for the given appointment id. */
  function openDetail(id: string) {
    activeDetailAppointmentId.value = id
  }

  /** Closes the detail view. */
  function closeDetail() {
    activeDetailAppointmentId.value = null
  }

  return {
    appointments,
    isCreateDialogOpen,
    activeDetailAppointmentId,
    fetchAppointments,
    createAppointment,
    moveAppointment,
    completeAppointment,
    reopenAppointment,
    deleteAppointment,
    openCreateDialog,
    closeCreateDialog,
    openDetail,
    closeDetail,
  }
})
