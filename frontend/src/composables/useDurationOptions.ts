import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { DURATION_OPTIONS } from '@/utils/appointmentSchedulingOptions'

/** Provides the selectable appointment durations with labels translated into the active locale. */
export function useDurationOptions() {
  const { t } = useI18n()

  const durationOptions = computed(() =>
    DURATION_OPTIONS.map((option) => ({
      ...option,
      label: t(`appointments.form.duration.options.${option.key}`),
    })),
  )

  return { durationOptions }
}
