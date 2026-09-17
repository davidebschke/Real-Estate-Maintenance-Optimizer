import { ref, watchEffect } from 'vue'
import { useTodaysAppointments } from '@/composables/useTodaysAppointments'
import { geocodeAddress, type GeocodedPosition } from '@/services/geocodingService'
import type { Appointment } from '@/types/appointment'

/** A single, geocoded stop on the appointment map, numbered like its `DailyAppointmentCard` counterpart. */
export interface AppointmentMapMarker {
  appointment: Appointment
  position: number
  lat: number
  lng: number
}

/** Resolves today's appointments to geocoded, chronologically ordered map markers. */
export function useAppointmentMapMarkers() {
  const { todaysAppointments } = useTodaysAppointments()
  const markers = ref<AppointmentMapMarker[]>([])
  const isLoading = ref(false)
  const hasGeocodingError = ref(false)

  watchEffect(async () => {
    const appointments = todaysAppointments.value
    isLoading.value = true

    const positionsByAddress = new Map<string, GeocodedPosition | null>()
    for (const address of new Set(appointments.map((appointment) => appointment.propertyAddress))) {
      positionsByAddress.set(address, await geocodeAddress(address))
    }

    const resolvedMarkers: AppointmentMapMarker[] = []
    appointments.forEach((appointment, index) => {
      const geocodedPosition = positionsByAddress.get(appointment.propertyAddress)
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
