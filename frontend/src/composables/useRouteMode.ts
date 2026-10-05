import { ref } from 'vue'
import type { RouteMode } from '@/types/route'

const routeMode = ref<RouteMode>('car')

/** Provides the way of travelling (by car or on foot) the routes are calculated for, shared by every component showing a route. */
export function useRouteMode() {
  return { routeMode }
}
