import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

/** Supported application locales. */
export type AppLocale = 'de' | 'en'

/** Provides reusable read/write access to the currently active application locale. */
export function useLocale() {
  const { locale } = useI18n()

  const currentLocale = computed<AppLocale>(() => locale.value as AppLocale)

  /** Switches the active application locale. */
  function setLocale(nextLocale: AppLocale): void {
    locale.value = nextLocale
  }

  /** Returns whether the given locale is currently active. */
  function isActive(candidateLocale: AppLocale): boolean {
    return currentLocale.value === candidateLocale
  }

  return { currentLocale, setLocale, isActive }
}
