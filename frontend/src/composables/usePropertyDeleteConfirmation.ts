import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import type { Property } from '@/types/property'

/** Confirms deletion of a property, warning that all of its appointments are deleted along with it. */
export function usePropertyDeleteConfirmation() {
  const confirm = useConfirm()
  const { t } = useI18n()

  /** Requests confirmation for deleting the given property, invoking `onConfirmed` if the user accepts. */
  function confirmDelete(property: Property, onConfirmed: () => void) {
    confirm.require({
      header: t('properties.card.deleteConfirm.header'),
      message: t('properties.card.deleteConfirm.message', { name: property.name }),
      acceptLabel: t('properties.card.deleteConfirm.confirm'),
      rejectLabel: t('properties.card.deleteConfirm.cancel'),
      acceptProps: { severity: 'danger' },
      rejectProps: { severity: 'secondary', text: true },
      accept: () => onConfirmed(),
    })
  }

  return { confirmDelete }
}
