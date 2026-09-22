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

  /** Creates a new property and inserts it into the store in name order, recording whether the request failed. */
  async function createProperty(payload: CreatePropertyPayload): Promise<boolean> {
    try {
      const created = await propertyService.createProperty(payload)
      latestChangeToken++
      properties.value = [...properties.value, created].sort((a, b) => a.name.localeCompare(b.name))
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
