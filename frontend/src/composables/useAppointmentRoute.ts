import { computed, ref, watch, type Ref } from 'vue'
import type { AppointmentMapMarker } from '@/composables/useAppointmentMapMarkers'
import { fetchRoute } from '@/services/routingService'
import type { RoadRoute, RouteLeg } from '@/types/route'

const NO_TRAVEL: RouteLeg = { distanceMeters: 0, durationSeconds: 0 }

/** Builds a comparable key for a marker's position. */
function positionKey(marker: AppointmentMapMarker): string {
  return `${marker.lat},${marker.lng}`
}

/** Whether the marker lies at the same position as the marker before it, i.e. needs no travel. */
function isAtPositionOf(
  marker: AppointmentMapMarker,
  previous: AppointmentMapMarker | undefined,
): boolean {
  return previous !== undefined && positionKey(marker) === positionKey(previous)
}

/**
 * Calculates the road route through the given map markers in order and assigns each appointment the leg driven to
 * reach it; consecutive appointments at the same position share a stop and get no travel, and the route is only
 * requested again when the sequence of positions changes. If the route cannot be calculated, `route` stays null and
 * `hasRouteError` is set so callers can keep drawing straight lines instead.
 */
export function useAppointmentRoute(markers: Ref<AppointmentMapMarker[]>) {
  const route = ref<RoadRoute | null>(null)
  const hasRouteError = ref(false)
  const isLoading = ref(false)
  const routedStopsKey = ref<string | null>(null)
  let latestRequest = 0

  const stops = computed(() =>
    markers.value.filter((marker, index, all) => !isAtPositionOf(marker, all[index - 1])),
  )
  const stopsKey = computed(() => stops.value.map(positionKey).join(';'))

  watch(
    stopsKey,
    async (key) => {
      const request = ++latestRequest
      route.value = null
      routedStopsKey.value = null
      hasRouteError.value = false
      if (stops.value.length < 2) {
        isLoading.value = false
        return
      }

      isLoading.value = true
      try {
        const calculatedRoute = await fetchRoute(stops.value.map(({ lat, lng }) => ({ lat, lng })))
        if (request !== latestRequest) return
        route.value = calculatedRoute
        routedStopsKey.value = key
      } catch {
        if (request !== latestRequest) return
        hasRouteError.value = true
      } finally {
        if (request === latestRequest) isLoading.value = false
      }
    },
    { immediate: true },
  )

  const legsByAppointmentId = computed(() => {
    const legs = new Map<string, RouteLeg>()
    if (!route.value || routedStopsKey.value !== stopsKey.value) {
      return legs
    }

    let stopIndex = 0
    markers.value.forEach((marker, index, all) => {
      if (index === 0) return
      if (isAtPositionOf(marker, all[index - 1])) {
        legs.set(marker.appointment.id, NO_TRAVEL)
        return
      }
      stopIndex += 1
      const leg = route.value?.legs[stopIndex - 1]
      if (leg) legs.set(marker.appointment.id, leg)
    })
    return legs
  })

  return { route, hasRouteError, isLoading, legsByAppointmentId }
}
