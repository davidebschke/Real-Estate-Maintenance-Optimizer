import { ref } from 'vue'

/** Identifier of a single top-level navigation entry. */
export type NavigationKey = 'overview' | 'calendar' | 'statistics' | 'properties'

/** Shared popup visibility state so every consumer of this composable observes the same value. */
const isPopupVisible = ref(false)

/** Provides reusable state and behavior for the header navigation and its "coming soon" popup. */
export function useNavigation() {
  /** Opens the "coming soon" popup, since target pages are not implemented yet. */
  function selectNavigationEntry(_key: NavigationKey): void {
    isPopupVisible.value = true
  }

  /** Closes the "coming soon" popup. */
  function closePopup(): void {
    isPopupVisible.value = false
  }

  return { isPopupVisible, selectNavigationEntry, closePopup }
}
