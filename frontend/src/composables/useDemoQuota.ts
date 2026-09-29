import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { DemoQuotaResource } from '@/types/auth'

/** Exposes how many more of the given resource the logged-in demo account may create; unlimited (null) for regular accounts. */
export function useDemoQuota(resource: DemoQuotaResource) {
  const authStore = useAuthStore()

  const remaining = computed<number | null>(() => {
    const user = authStore.currentUser
    if (!user?.demoAccount) return null
    return resource === 'properties' ? user.remainingPropertyCreations : user.remainingAppointmentCreations
  })

  const isLimited = computed(() => remaining.value !== null)
  const isExhausted = computed(() => remaining.value !== null && remaining.value <= 0)

  return { remaining, isLimited, isExhausted }
}
