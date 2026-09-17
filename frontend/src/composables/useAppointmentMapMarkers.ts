import { ref, watchEffect } from 'vue'
import { useTodaysAppointments } from '@/composables/useTodaysAppointments'
import { usePropertiesStore } from '@/stores/properties'
import { geocodeAddress, type GeocodedPosition } from '@/services/geocodingService'
import type { Appointment } from '@/types/appointment'

/** A single, geocoded stop on the appointment map, numbered like its `DailyAppointmentCard` counterpart. */
export interface AppointmentMapMarker {
  appointment: Appointment
  position: number
  lat: number
  lng: number
}

/**
 * Resolves today's appointments to chronologically ordered map markers, preferring each appointment's
 * property's stored coordinates and falling back to geocoding its address only when those are missing.
 */
export function useAppointmentMapMarkers() {
  const { todaysAppointments } = useTodaysAppointments()
  const propertiesStore = usePropertiesStore()
  const markers = ref<AppointmentMapMarker[]>([])
  const isLoading = ref(false)
  const hasGeocodingError = ref(false)

  watchEffect(async () => {
    const appointments = todaysAppointments.value
    const properties = propertiesStore.properties
    isLoading.value = true

    const positionsByPropertyId = new Map<string, GeocodedPosition | null>()
    for (const appointment of appointments) {
      if (positionsByPropertyId.has(appointment.propertyId)) {
        continue
      }
      const property = properties.find((candidate) => candidate.id === appointment.propertyId)
      const storedPosition =
        property?.latitude != null && property?.longitude != null
          ? { lat: property.latitude, lng: property.longitude }
          : null
      positionsByPropertyId.set(
        appointment.propertyId,
        storedPosition ?? (await geocodeAddress(appointment.propertyAddress)),
      )
    }

    const resolvedMarkers: AppointmentMapMarker[] = []
    appointments.forEach((appointment, index) => {
      const geocodedPosition = positionsByPropertyId.get(appointment.propertyId)
      if (geocodedPosition) {
        resolvedMarkers.push({ appointment, position: index + 1, ...geocodedPosition })
      }
    })

    markers.value = resolvedMarkers
    hasGeocodingError.value =
      appointments.length > 0 && resolvedMarkers.length < appointments.length
    isLoading.value = false
  })

  return { markers, isLoading, hasGeocodingError }
}
