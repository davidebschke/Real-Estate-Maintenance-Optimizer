import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import type { Tenant } from '@/types/apartment'

/** Confirms deletion of a tenant, noting that their rent payments stay as payments of the previous tenant. */
export function useTenantDeleteConfirmation() {
  const confirm = useConfirm()
  const { t } = useI18n()

  /** Requests confirmation for deleting the given tenant, invoking `onConfirmed` if the user accepts. */
  function confirmDelete(tenant: Tenant, onConfirmed: () => void) {
    confirm.require({
      header: t('tenants.deleteConfirm.header'),
      message: t('tenants.deleteConfirm.message', { name: `${tenant.firstName} ${tenant.lastName}` }),
      acceptLabel: t('tenants.deleteConfirm.confirm'),
      rejectLabel: t('tenants.deleteConfirm.cancel'),
      acceptProps: { severity: 'danger' },
      rejectProps: { severity: 'secondary', text: true },
      accept: () => onConfirmed(),
    })
  }

  return { confirmDelete }
}
