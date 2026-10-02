import { useLocale } from '@/composables/useLocale'
import type { AppointmentConflict } from '@/services/appointmentService'
import { formatDayOption } from '@/utils/appointmentSchedulingOptions'
import { formatLocalizedTime } from '@/utils/dateFormat'

/** Provides a formatter turning a conflict's suggested next free slot into one readable "day, time" line in the active locale. */
export function useSuggestedSlotLabel() {
  const { currentLocale } = useLocale()

  /** Formats the conflict's suggested start, e.g. "Mi., 12.08., 15:15". */
  function formatSuggestedSlot(conflict: AppointmentConflict): string {
    const { label } = formatDayOption(conflict.suggestedStart, currentLocale.value)
    return `${label}, ${formatLocalizedTime(conflict.suggestedStart, currentLocale.value)}`
  }

  return { formatSuggestedSlot }
}
