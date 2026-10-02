import { ref } from 'vue'
import { toAccountFailureReason } from '@/services/accountService'
import type { AccountFailureReason } from '@/types/auth'

/** Tracks the saving state, success and failure reason of one profile settings form around the given save action. */
export function useSettingsSubmission<Input extends unknown[]>(save: (...input: Input) => Promise<unknown>) {
  const isSaving = ref(false)
  const isSaved = ref(false)
  const failureReason = ref<AccountFailureReason | null>(null)

  /** Runs the save action once at a time and resolves to whether it succeeded. */
  async function submit(...input: Input): Promise<boolean> {
    if (isSaving.value) return false
    isSaving.value = true
    isSaved.value = false
    failureReason.value = null
    try {
      await save(...input)
      isSaved.value = true
      return true
    } catch (error) {
      failureReason.value = toAccountFailureReason(error)
      return false
    } finally {
      isSaving.value = false
    }
  }

  /** Forgets a previous success or failure, e.g. once the user edits the form again. */
  function clearFeedback(): void {
    isSaved.value = false
    failureReason.value = null
  }

  return { isSaving, isSaved, failureReason, submit, clearFeedback }
}
