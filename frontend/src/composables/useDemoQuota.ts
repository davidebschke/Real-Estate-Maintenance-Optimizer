import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { CurrentUser, DemoQuotaResource } from '@/types/auth'

/** Maps each limited resource to the number of creations or uses the given account has left. */
function remainingByResource(user: CurrentUser): Record<DemoQuotaResource, number | null> {
  return {
    properties: user.remainingPropertyCreations,
    appointments: user.remainingAppointmentCreations,
    tenants: user.remainingTenantCreations,
    aiOptimizations: user.remainingAiOptimizations,
  }
}

/** Exposes how many more of the given resource the logged-in demo account may create or use; unlimited (null) for regular accounts. */
export function useDemoQuota(resource: DemoQuotaResource) {
  const authStore = useAuthStore()

  const remaining = computed<number | null>(() => {
    const user = authStore.currentUser
    if (!user?.demoAccount) return null
    return remainingByResource(user)[resource]
  })

  const isLimited = computed(() => remaining.value !== null)
  const isExhausted = computed(() => remaining.value !== null && remaining.value <= 0)

  return { remaining, isLimited, isExhausted }
}
