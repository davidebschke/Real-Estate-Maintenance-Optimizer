import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import type { AppointmentDeleteScope } from '@/services/appointmentService'
import type { Appointment } from '@/types/appointment'

/** Confirms deletion of an appointment, asking whether all following occurrences should be deleted too when it recurs. */
export function useAppointmentDeleteConfirmation() {
  const confirm = useConfirm()
  const { t } = useI18n()

  /** Requests confirmation for deleting the given appointment, invoking `onConfirmed` with the chosen scope. */
  function confirmDelete(
    appointment: Appointment,
    onConfirmed: (scope: AppointmentDeleteScope) => void,
  ) {
    if (!appointment.recurring) {
      confirm.require({
        header: t('appointments.detail.deleteConfirm.header'),
        message: t('appointments.detail.deleteConfirm.singleMessage'),
        acceptLabel: t('appointments.detail.deleteConfirm.confirm'),
        rejectLabel: t('appointments.detail.deleteConfirm.cancel'),
        accept: () => onConfirmed('single'),
      })
      return
    }

    confirm.require({
      header: t('appointments.detail.deleteConfirm.header'),
      message: t('appointments.detail.deleteConfirm.recurringMessage'),
      acceptLabel: t('appointments.detail.deleteConfirm.allFollowing'),
      rejectLabel: t('appointments.detail.deleteConfirm.onlyThisOne'),
      accept: () => onConfirmed('series'),
      reject: () => onConfirmed('single'),
    })
  }

  return { confirmDelete }
}
