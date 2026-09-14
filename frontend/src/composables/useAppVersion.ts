import { ref } from 'vue'
import { fetchAppVersion } from '@/services/versionService'

/** Provides reactive, reusable access to the deployed backend application version. */
export function useAppVersion() {
  const version = ref<string | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  /** Loads the application version from the backend. */
  async function loadVersion(): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      version.value = await fetchAppVersion()
    } catch {
      error.value = 'Failed to load application version.'
    } finally {
      isLoading.value = false
    }
  }

  return { version, isLoading, error, loadVersion }
}
