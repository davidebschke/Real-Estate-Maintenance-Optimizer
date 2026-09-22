import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as propertyService from '@/services/propertyService'
import type { CreatePropertyPayload, Property } from '@/types/property'

/** Holds every property (real-estate object) shared across the app. */
export const usePropertiesStore = defineStore('properties', () => {
  const properties = ref<Property[]>([])
  const hasLoadError = ref(false)
  const hasCreateError = ref(false)
  const isCreateDialogOpen = ref(false)

  /** Loads every property from the backend, recording whether the request failed. */
  async function fetchProperties() {
    try {
      properties.value = await propertyService.fetchProperties()
      hasLoadError.value = false
    } catch {
      hasLoadError.value = true
    }
  }

  /** Creates a new property and appends it to the store, recording whether the request failed. */
  async function createProperty(payload: CreatePropertyPayload): Promise<boolean> {
    try {
      const created = await propertyService.createProperty(payload)
      properties.value = [...properties.value, created]
      hasCreateError.value = false
      return true
    } catch {
      hasCreateError.value = true
      return false
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

  return {
    properties,
    hasLoadError,
    hasCreateError,
    isCreateDialogOpen,
    fetchProperties,
    createProperty,
    openCreateDialog,
    closeCreateDialog,
  }
})
