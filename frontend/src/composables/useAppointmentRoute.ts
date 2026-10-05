import axios from 'axios'
import { computed, ref, watch, type Ref } from 'vue'
import type { AppointmentMapMarker } from '@/composables/useAppointmentMapMarkers'
import { useRouteMode } from '@/composables/useRouteMode'
import { fetchRoute } from '@/services/routingService'
import type { RoadRoute, RouteLeg } from '@/types/route'

const ROUTING_DISABLED_STATUS = 501

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

/** Whether the failed route request was answered by a server that has routing deliberately switched off. */
function isRoutingDisabled(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === ROUTING_DISABLED_STATUS
}

/**
 * Calculates the route for the selected mode through the given map markers in order and assigns each appointment the leg driven to reach it, requesting it again only when the mode or the sequence of positions changes.
 * Consecutive appointments at the same position share a stop and get no leg; if the route cannot be calculated `route` stays null and `hasRouteError` is set, except when the server has routing deliberately disabled.
 */
export function useAppointmentRoute(markers: Ref<AppointmentMapMarker[]>) {
  const { routeMode } = useRouteMode()
  const route = ref<RoadRoute | null>(null)
  const hasRouteError = ref(false)
  const routedStopsKey = ref<string | null>(null)
  let latestRequest = 0

  const stops = computed(() =>
    markers.value.filter((marker, index, all) => !isAtPositionOf(marker, all[index - 1])),
  )
  const stopsKey = computed(() => `${routeMode.value}|${stops.value.map(positionKey).join(';')}`)

  watch(
    stopsKey,
    async (key) => {
      const request = ++latestRequest
      route.value = null
      routedStopsKey.value = null
      hasRouteError.value = false
      if (stops.value.length < 2) {
        return
      }

      try {
        const calculatedRoute = await fetchRoute(
          stops.value.map(({ lat, lng }) => ({ lat, lng })),
          routeMode.value,
        )
        if (request !== latestRequest) return
        route.value = calculatedRoute
        routedStopsKey.value = key
      } catch (error) {
        if (request !== latestRequest) return
        hasRouteError.value = !isRoutingDisabled(error)
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
        return
      }
      stopIndex += 1
      const leg = route.value?.legs[stopIndex - 1]
      if (leg) legs.set(marker.appointment.id, leg)
    })
    return legs
  })

  return { route, hasRouteError, legsByAppointmentId }
}
