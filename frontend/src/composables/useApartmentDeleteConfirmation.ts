import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import { useApartmentFormatting } from '@/composables/useApartmentFormatting'
import type { Apartment } from '@/types/apartment'

/** Confirms deletion of an apartment, warning that its tenants, their rent payments and the rent history are deleted along with it. */
export function useApartmentDeleteConfirmation() {
  const confirm = useConfirm()
  const { t } = useI18n()
  const { formatFloor, formatArea } = useApartmentFormatting()

  /** Requests confirmation for deleting the given apartment, invoking `onConfirmed` if the user accepts. */
  function confirmDelete(apartment: Apartment, onConfirmed: () => void) {
    confirm.require({
      header: t('tenants.apartmentDeleteConfirm.header'),
      message: t('tenants.apartmentDeleteConfirm.message', {
        name: `${formatFloor(apartment.floor)}, ${formatArea(apartment.areaSquareMeters)}`,
      }),
      acceptLabel: t('tenants.apartmentDeleteConfirm.confirm'),
      rejectLabel: t('tenants.apartmentDeleteConfirm.cancel'),
      acceptProps: { severity: 'danger' },
      rejectProps: { severity: 'secondary', text: true },
      accept: () => onConfirmed(),
    })
  }

  return { confirmDelete }
}
