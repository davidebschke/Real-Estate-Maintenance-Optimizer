import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useLocale } from '@/composables/useLocale'
import { formatLocalizedTime } from '@/utils/dateFormat'
import { toInitials } from '@/utils/initials'

/** Derives how the logged-in account is shown: its (for demo accounts localized) name, initials, role line and session end time. */
export function useAccountPresentation() {
  const { t } = useI18n()
  const { currentLocale } = useLocale()
  const authStore = useAuthStore()

  const sessionEndTime = computed(() => {
    const expiresAt = authStore.currentUser?.expiresAt
    return expiresAt ? formatLocalizedTime(expiresAt, currentLocale.value) : ''
  })

  const displayName = computed(() => {
    const user = authStore.currentUser
    if (!user) return ''
    return user.demoAccount ? t('auth.account.demoDisplayName') : user.displayName
  })

  const initials = computed(() => toInitials(displayName.value))

  const roleLabel = computed(() => {
    const user = authStore.currentUser
    if (!user) return ''
    return user.demoAccount
      ? t('auth.account.demoRole', { time: sessionEndTime.value })
      : t('auth.account.regularRole', { username: user.username })
  })

  return { displayName, initials, roleLabel, sessionEndTime }
}
