import { computed, ref, type Ref } from 'vue'
import { defineStore } from 'pinia'
import * as apartmentService from '@/services/apartmentService'
import type { Apartment, ApartmentWithTenantPayload, Tenant, TenantDetails } from '@/types/apartment'

/** What the shared tenant form is currently doing: creating a tenant with a new apartment, adding a tenant to an existing apartment or editing a tenant together with their apartment. */
export type TenantFormTarget =
  | { mode: 'create'; propertyId: string }
  | { mode: 'add'; apartment: Apartment }
  | { mode: 'edit'; apartment: Apartment; tenant: Tenant }

/** Holds the apartments and tenants of every property whose detail view was opened, plus the state of the shared tenant form. */
export const useTenantsStore = defineStore('tenants', () => {
  const apartmentsByProperty = ref<Record<string, Apartment[]>>({})
  const hasLoadError = ref(false)
  const hasSaveError = ref(false)
  const hasDeleteError = ref(false)
  const formTarget = ref<TenantFormTarget | null>(null)
  /** Bumped by every fetch and by reset(), so a response that arrives after a newer fetch or an account change is discarded. */
  let latestChangeToken = 0
  /** Bumped by reset(), so a save or delete answered only after the account changed never touches the new account's data. */
  let sessionEpoch = 0

  /** Loads the apartments of the given property from the backend, recording whether the request failed and clearing a stale delete error; ignores the result if a newer fetch or an account change already happened. */
  async function fetchApartments(propertyId: string) {
    const requestToken = ++latestChangeToken
    hasDeleteError.value = false
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

  /** Sends one change to the backend, recording on the given error flag whether it failed, and reloads the property's apartments afterwards so their order and content stay authoritative. */
  async function applyChange(propertyId: string, request: () => Promise<unknown>, errorFlag: Ref<boolean>): Promise<boolean> {
    const requestEpoch = sessionEpoch
    try {
      await request()
    } catch {
      errorFlag.value = true
      return false
    }
    if (requestEpoch !== sessionEpoch) return false
    errorFlag.value = false
    await fetchApartments(propertyId)
    return true
  }

  /** Creates a new apartment in the given property together with its first tenant. */
  function createApartment(propertyId: string, payload: ApartmentWithTenantPayload): Promise<boolean> {
    return applyChange(propertyId, () => apartmentService.createApartment(propertyId, payload), hasSaveError)
  }

  /** Adds a further tenant to the given apartment. */
  function addTenant(propertyId: string, apartmentId: string, payload: TenantDetails): Promise<boolean> {
    return applyChange(propertyId, () => apartmentService.addTenant(apartmentId, payload), hasSaveError)
  }

  /** Updates the given tenant together with the data of their apartment. */
  function updateTenant(propertyId: string, tenantId: string, payload: ApartmentWithTenantPayload): Promise<boolean> {
    return applyChange(propertyId, () => apartmentService.updateTenant(tenantId, payload), hasSaveError)
  }

  /** Deletes the given tenant, which also removes their apartment if they were its last tenant. */
  function deleteTenant(propertyId: string, tenantId: string): Promise<boolean> {
    return applyChange(propertyId, () => apartmentService.deleteTenant(tenantId), hasDeleteError)
  }

  /** Opens the tenant form for creating a tenant together with a new apartment in the given property. */
  function openCreateDialog(propertyId: string) {
    hasSaveError.value = false
    formTarget.value = { mode: 'create', propertyId }
  }

  /** Opens the tenant form for adding a further tenant to the given apartment. */
  function openAddTenantDialog(apartment: Apartment) {
    hasSaveError.value = false
    formTarget.value = { mode: 'add', apartment }
  }

  /** Opens the tenant form pre-filled for editing the given tenant together with their apartment. */
  function openEditDialog(apartment: Apartment, tenant: Tenant) {
    hasSaveError.value = false
    formTarget.value = { mode: 'edit', apartment, tenant }
  }

  /** Closes the tenant form. */
  function closeFormDialog() {
    formTarget.value = null
  }

  /** Whether the shared tenant form should be visible; closing it also clears its target. */
  const isFormDialogOpen = computed({
    get: () => formTarget.value !== null,
    set: (value: boolean) => {
      if (!value) closeFormDialog()
    },
  })

  /** Forgets every apartment, error flag and open form, and ignores any still in-flight request, e.g. when the logged-in account changes. */
  function reset() {
    sessionEpoch++
    latestChangeToken++
    apartmentsByProperty.value = {}
    hasLoadError.value = false
    hasSaveError.value = false
    hasDeleteError.value = false
    formTarget.value = null
  }

  return {
    apartmentsByProperty,
    hasLoadError,
    hasSaveError,
    hasDeleteError,
    formTarget,
    isFormDialogOpen,
    fetchApartments,
    createApartment,
    addTenant,
    updateTenant,
    deleteTenant,
    openCreateDialog,
    openAddTenantDialog,
    openEditDialog,
    closeFormDialog,
    reset,
  }
})
