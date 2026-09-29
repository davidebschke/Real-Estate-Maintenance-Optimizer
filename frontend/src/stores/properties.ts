import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as propertyService from '@/services/propertyService'
import { useAppointmentsStore } from '@/stores/appointments'
import { useAuthStore } from '@/stores/auth'
import type { CreatePropertyPayload, Property } from '@/types/property'

/** Holds every property (real-estate object) shared across the app. */
export const usePropertiesStore = defineStore('properties', () => {
  const properties = ref<Property[]>([])
  const hasLoadError = ref(false)
  const hasCreateError = ref(false)
  const hasUpdateError = ref(false)
  const hasDeleteError = ref(false)
  const isCreateDialogOpen = ref(false)
  const editingProperty = ref<Property | null>(null)
  /** Bumped by every state-changing operation so an in-flight fetchProperties() started before it cannot overwrite its result once that fetch resolves. */
  let latestChangeToken = 0

  /** Loads every property from the backend, recording whether the request failed; ignores the result if a newer change (fetch or create) already happened. */
  async function fetchProperties() {
    const requestToken = ++latestChangeToken
    try {
      const fetched = await propertyService.fetchProperties()
      if (requestToken !== latestChangeToken) return
      properties.value = fetched
      hasLoadError.value = false
    } catch {
      if (requestToken !== latestChangeToken) return
      hasLoadError.value = true
    }
  }

  /** Creates a new property and inserts it into the store in name order, recording whether the request failed and, for a demo account, refreshing its remaining creation limit. */
  async function createProperty(payload: CreatePropertyPayload): Promise<boolean> {
    try {
      const created = await propertyService.createProperty(payload)
      latestChangeToken++
      properties.value = [...properties.value, created].sort((a, b) => a.name.localeCompare(b.name))
      hasCreateError.value = false
    } catch {
      hasCreateError.value = true
      return false
    }
    await useAuthStore().refreshDemoQuota()
    return true
  }

  /** Updates a property and replaces it in the store, re-sorting by name, recording whether the request failed. */
  async function updateProperty(id: string, payload: CreatePropertyPayload): Promise<boolean> {
    try {
      const updated = await propertyService.updateProperty(id, payload)
      latestChangeToken++
      properties.value = properties.value
        .map((property) => (property.id === id ? updated : property))
        .sort((a, b) => a.name.localeCompare(b.name))
      hasUpdateError.value = false
      return true
    } catch {
      hasUpdateError.value = true
      return false
    }
  }

  /** Deletes a property and every appointment referencing it, recording whether the delete request itself failed; a failure to refresh the appointments store afterwards does not count as a delete failure. */
  async function deleteProperty(id: string): Promise<boolean> {
    try {
      await propertyService.deleteProperty(id)
    } catch {
      hasDeleteError.value = true
      return false
    }
    latestChangeToken++
    properties.value = properties.value.filter((property) => property.id !== id)
    hasDeleteError.value = false
    await refreshAppointmentsAfterDelete()
    return true
  }

  /** Re-fetches the appointments store after a cascading delete, swallowing a failure since the deletion itself already succeeded. */
  async function refreshAppointmentsAfterDelete() {
    try {
      await useAppointmentsStore().fetchAppointments()
    } catch {
      return
    }
  }

  /** Opens the property creation form. */
  function openCreateDialog() {
    isCreateDialogOpen.value = true
  }

  /** Closes the property creation form. */
  function closeCreateDialog() {
    isCreateDialogOpen.value = false
  }

  /** Opens the property form pre-filled for editing the given property. */
  function openEditDialog(property: Property) {
    editingProperty.value = property
  }

  /** Closes the property edit form. */
  function closeEditDialog() {
    editingProperty.value = null
  }

  /** Forgets every property, error flag and open form, and ignores any still in-flight fetch, e.g. when the logged-in account changes. */
  function reset() {
    latestChangeToken++
    properties.value = []
    hasLoadError.value = false
    hasCreateError.value = false
    hasUpdateError.value = false
    hasDeleteError.value = false
    isCreateDialogOpen.value = false
    editingProperty.value = null
  }

  /** Whether the shared property form dialog (create or edit) should be visible; closing it also resets both modes. */
  const isFormDialogOpen = computed({
    get: () => isCreateDialogOpen.value || editingProperty.value !== null,
    set: (value: boolean) => {
      if (value) return
      closeCreateDialog()
      closeEditDialog()
    },
  })

  return {
    properties,
    hasLoadError,
    hasCreateError,
    hasUpdateError,
    hasDeleteError,
    isCreateDialogOpen,
    editingProperty,
    isFormDialogOpen,
    fetchProperties,
    createProperty,
    updateProperty,
    deleteProperty,
    openCreateDialog,
    closeCreateDialog,
    openEditDialog,
    closeEditDialog,
    reset,
  }
})
