import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as propertyService from '@/services/propertyService'
import type { Property } from '@/types/property'

/** Holds every property (real-estate object) shared across the app. */
export const usePropertiesStore = defineStore('properties', () => {
  const properties = ref<Property[]>([])
  const hasLoadError = ref(false)

  /** Loads every property from the backend, recording whether the request failed. */
  async function fetchProperties() {
    try {
      properties.value = await propertyService.fetchProperties()
      hasLoadError.value = false
    } catch {
      hasLoadError.value = true
    }
  }

  return {
    properties,
    hasLoadError,
    fetchProperties,
  }
})
