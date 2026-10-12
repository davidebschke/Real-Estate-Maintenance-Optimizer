import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'

/** Confirms deactivating the rent payment check of a property, warning that its payments are no longer judged until it is activated again. */
export function useTrackingDeactivationConfirmation() {
  const confirm = useConfirm()
  const { t } = useI18n()

  /** Requests confirmation for deactivating the rent payment check, invoking `onConfirmed` if the user accepts. */
  function confirmDeactivation(onConfirmed: () => void) {
    confirm.require({
      header: t('payments.tracking.deactivateConfirm.header'),
      message: t('payments.tracking.deactivateConfirm.message'),
      acceptLabel: t('payments.tracking.deactivateConfirm.confirm'),
      rejectLabel: t('payments.tracking.deactivateConfirm.cancel'),
      acceptProps: { severity: 'danger' },
      rejectProps: { severity: 'secondary', text: true },
      accept: () => onConfirmed(),
    })
  }

  return { confirmDeactivation }
}
