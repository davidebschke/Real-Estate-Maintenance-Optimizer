import { computed, ref, type Ref } from 'vue'
import { defineStore } from 'pinia'
import * as apartmentService from '@/services/apartmentService'
import { useAuthStore } from '@/stores/auth'
import { usePaymentsStore } from '@/stores/payments'
import { usePropertiesStore } from '@/stores/properties'
import { getServerErrorMessage } from '@/utils/serverErrorMessage'
import type {
  Apartment,
  ApartmentUpdatePayload,
  NewApartmentPayload,
  Tenant,
  TenantDetails,
} from '@/types/apartment'

/** What the apartment form is currently doing: creating an apartment in a property or editing an existing one. */
export type ApartmentFormTarget = { mode: 'create'; propertyId: string } | { mode: 'edit'; apartment: Apartment }

/** What the tenant form is currently doing: adding a tenant to an apartment or renaming an existing tenant. */
export type TenantFormTarget = { mode: 'add'; apartment: Apartment } | { mode: 'edit'; apartment: Apartment; tenant: Tenant }

/** Holds the apartments and tenants of every property whose detail view was opened, plus the state of the apartment and tenant forms. */
export const useTenantsStore = defineStore('tenants', () => {
  const apartmentsByProperty = ref<Record<string, Apartment[]>>({})
  const hasLoadError = ref(false)
  const hasSaveError = ref(false)
  const hasDeleteError = ref(false)
  const hasApartmentDeleteError = ref(false)
  /** The localized message the backend gave for the last failed save or delete, e.g. a reached size limit, or null if it gave none. */
  const lastChangeErrorMessage = ref<string | null>(null)
  const apartmentFormTarget = ref<ApartmentFormTarget | null>(null)
  const tenantFormTarget = ref<TenantFormTarget | null>(null)
  /** Bumped by every fetch and by reset(), so a response that arrives after a newer fetch or an account change is discarded. */
  let latestChangeToken = 0
  /** Bumped by reset(), so a save or delete answered only after the account changed never touches the new account's data. */
  let sessionEpoch = 0

  /** Loads the apartments of the given property from the backend, recording whether the request failed and clearing stale load and delete errors of a previously viewed property; ignores the result if a newer fetch or an account change already happened. */
  async function fetchApartments(propertyId: string) {
    const requestToken = ++latestChangeToken
    hasLoadError.value = false
    hasDeleteError.value = false
    hasApartmentDeleteError.value = false
    try {
      const fetched = await apartmentService.fetchApartments(propertyId)
      if (requestToken !== latestChangeToken) return
      apartmentsByProperty.value = { ...apartmentsByProperty.value, [propertyId]: fetched }
      hasLoadError.value = false
    } catch {
      if (requestToken !== latestChangeToken) return
      hasLoadError.value = true
    }
  }

  /** Sends one change to the backend, recording on the given error flag whether it failed, and reloads the property's apartments and the properties (whose tenant counts changed) afterwards so their order and content stay authoritative. */
  async function applyChange(propertyId: string, request: () => Promise<unknown>, errorFlag: Ref<boolean>): Promise<boolean> {
    const requestEpoch = sessionEpoch
    try {
      await request()
    } catch (error) {
      errorFlag.value = true
      lastChangeErrorMessage.value = getServerErrorMessage(error)
      return false
    }
    if (requestEpoch !== sessionEpoch) return false
    errorFlag.value = false
    lastChangeErrorMessage.value = null
    await Promise.all([fetchApartments(propertyId), usePropertiesStore().fetchProperties(), reloadLoadedPayments(propertyId)])
    return true
  }

  /** Reloads the rent payment table and alerts of a property whose table was already loaded, since every apartment or tenant change can alter what they show (new apartments, a changed rent, payments of a deleted tenant or apartment). */
  async function reloadLoadedPayments(propertyId: string) {
    const paymentsStore = usePaymentsStore()
    if (paymentsStore.overviewsByProperty[propertyId]) {
      await Promise.all([paymentsStore.fetchYear(propertyId), paymentsStore.fetchAlerts()])
    }
  }

  /** Forgets the cached apartments of a property that no longer exists. */
  function forgetProperty(propertyId: string) {
    apartmentsByProperty.value = Object.fromEntries(
      Object.entries(apartmentsByProperty.value).filter(([cachedPropertyId]) => cachedPropertyId !== propertyId),
    )
  }

  /** Forgets the cached data of one apartment, e.g. because it was deleted. */
  function forgetApartment(propertyId: string, apartmentId: string) {
    const cached = apartmentsByProperty.value[propertyId]
    if (!cached) return
    apartmentsByProperty.value = {
      ...apartmentsByProperty.value,
      [propertyId]: cached.filter((apartment) => apartment.id !== apartmentId),
    }
  }

  /** Sends a change that creates a tenant and, for a demo account, refreshes its remaining tenant creations afterwards. */
  async function applyTenantCreation(propertyId: string, request: () => Promise<unknown>): Promise<boolean> {
    const succeeded = await applyChange(propertyId, request, hasSaveError)
    if (succeeded) await useAuthStore().refreshDemoQuota()
    return succeeded
  }

  /** Creates a new apartment in the given property, optionally together with its first tenant, which is the only part that uses up a demo account's tenant creations. */
  function createApartment(propertyId: string, payload: NewApartmentPayload): Promise<boolean> {
    const request = () => apartmentService.createApartment(propertyId, payload)
    return payload.tenant ? applyTenantCreation(propertyId, request) : applyChange(propertyId, request, hasSaveError)
  }

  /** Updates the data of the given apartment, applying a changed total rent from the payload's month. */
  function updateApartment(propertyId: string, apartmentId: string, payload: ApartmentUpdatePayload): Promise<boolean> {
    return applyChange(propertyId, () => apartmentService.updateApartment(apartmentId, payload), hasSaveError)
  }

  /** Deletes the given apartment together with its tenants, rent payments and rent history and drops it from the cache. */
  function deleteApartment(propertyId: string, apartmentId: string): Promise<boolean> {
    return applyChange(
      propertyId,
      async () => {
        await apartmentService.deleteApartment(apartmentId)
        forgetApartment(propertyId, apartmentId)
      },
      hasApartmentDeleteError,
    )
  }

  /** Adds a further tenant to the given apartment. */
  function addTenant(propertyId: string, apartmentId: string, payload: TenantDetails): Promise<boolean> {
    return applyTenantCreation(propertyId, () => apartmentService.addTenant(apartmentId, payload))
  }

  /** Renames the given tenant. */
  function updateTenant(propertyId: string, tenantId: string, payload: TenantDetails): Promise<boolean> {
    return applyChange(propertyId, () => apartmentService.updateTenant(tenantId, payload), hasSaveError)
  }

  /** Deletes the given tenant, whose apartment stays vacant if they were its last tenant. */
  function deleteTenant(propertyId: string, tenantId: string): Promise<boolean> {
    return applyChange(propertyId, () => apartmentService.deleteTenant(tenantId), hasDeleteError)
  }

  /** Clears the outcome of the last save so a freshly opened form starts without an error. */
  function clearSaveError() {
    hasSaveError.value = false
    lastChangeErrorMessage.value = null
  }

  /** Opens the apartment form for creating an apartment in the given property. */
  function openCreateApartmentDialog(propertyId: string) {
    clearSaveError()
    apartmentFormTarget.value = { mode: 'create', propertyId }
  }

  /** Opens the apartment form pre-filled for editing the given apartment. */
  function openEditApartmentDialog(apartment: Apartment) {
    clearSaveError()
    apartmentFormTarget.value = { mode: 'edit', apartment }
  }

  /** Opens the tenant form for adding a tenant to the given apartment. */
  function openAddTenantDialog(apartment: Apartment) {
    clearSaveError()
    tenantFormTarget.value = { mode: 'add', apartment }
  }

  /** Opens the tenant form pre-filled for renaming the given tenant. */
  function openEditTenantDialog(apartment: Apartment, tenant: Tenant) {
    clearSaveError()
    tenantFormTarget.value = { mode: 'edit', apartment, tenant }
  }

  /** Closes the apartment form. */
  function closeApartmentFormDialog() {
    apartmentFormTarget.value = null
  }

  /** Closes the tenant form. */
  function closeTenantFormDialog() {
    tenantFormTarget.value = null
  }

  /** Whether the shared apartment form should be visible; closing it also clears its target. */
  const isApartmentFormDialogOpen = computed({
    get: () => apartmentFormTarget.value !== null,
    set: (value: boolean) => {
      if (!value) closeApartmentFormDialog()
    },
  })

  /** Whether the shared tenant form should be visible; closing it also clears its target. */
  const isTenantFormDialogOpen = computed({
    get: () => tenantFormTarget.value !== null,
    set: (value: boolean) => {
      if (!value) closeTenantFormDialog()
    },
  })

  /** Forgets every apartment, error flag and open form, and ignores any still in-flight request, e.g. when the logged-in account changes. */
  function reset() {
    sessionEpoch++
    latestChangeToken++
    apartmentsByProperty.value = {}
    hasLoadError.value = false
    hasSaveError.value = false
    lastChangeErrorMessage.value = null
    hasDeleteError.value = false
    hasApartmentDeleteError.value = false
    apartmentFormTarget.value = null
    tenantFormTarget.value = null
  }

  return {
    apartmentsByProperty,
    hasLoadError,
    hasSaveError,
    hasDeleteError,
    hasApartmentDeleteError,
    lastChangeErrorMessage,
    apartmentFormTarget,
    tenantFormTarget,
    isApartmentFormDialogOpen,
    isTenantFormDialogOpen,
    fetchApartments,
    createApartment,
    updateApartment,
    deleteApartment,
    addTenant,
    updateTenant,
    deleteTenant,
    forgetProperty,
    openCreateApartmentDialog,
    openEditApartmentDialog,
    openAddTenantDialog,
    openEditTenantDialog,
    closeApartmentFormDialog,
    closeTenantFormDialog,
    reset,
  }
})
