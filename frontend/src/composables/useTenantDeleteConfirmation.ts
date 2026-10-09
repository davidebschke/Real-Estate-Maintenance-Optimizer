import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import type { Tenant } from '@/types/apartment'

/** Confirms deletion of a tenant, warning that the apartment details are deleted along with the last tenant of an apartment. */
export function useTenantDeleteConfirmation() {
  const confirm = useConfirm()
  const { t } = useI18n()

  /** Requests confirmation for deleting the given tenant, invoking `onConfirmed` if the user accepts. */
  function confirmDelete(tenant: Tenant, isLastTenant: boolean, onConfirmed: () => void) {
    const messageKey = isLastTenant ? 'tenants.deleteConfirm.lastTenantMessage' : 'tenants.deleteConfirm.message'
    confirm.require({
      header: t('tenants.deleteConfirm.header'),
      message: t(messageKey, { name: `${tenant.firstName} ${tenant.lastName}` }),
      acceptLabel: t('tenants.deleteConfirm.confirm'),
      rejectLabel: t('tenants.deleteConfirm.cancel'),
      acceptProps: { severity: 'danger' },
      rejectProps: { severity: 'secondary', text: true },
      accept: () => onConfirmed(),
    })
  }

  return { confirmDelete }
}
