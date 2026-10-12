/** A person renting an apartment. */
export interface Tenant {
  id: string
  firstName: string
  lastName: string
}

/** The floor, area and rent figures of an apartment, shared by every tenant living in it. */
export interface ApartmentDetails {
  /** 0 is the ground floor, a negative number a basement level. */
  floor: number
  areaSquareMeters: number
  totalRent: number
  coldRent: number
  additionalCosts: number
}

/** An apartment of a property together with the tenants living in it, none while it is vacant. */
export interface Apartment extends ApartmentDetails {
  id: string
  propertyId: string
  tenants: Tenant[]
}

/** The personal data of a tenant. */
export type TenantDetails = Pick<Tenant, 'firstName' | 'lastName'>

/** Payload to create an apartment, optionally together with its first tenant (without one the apartment is vacant). */
export interface NewApartmentPayload {
  apartment: ApartmentDetails
  tenant?: TenantDetails
}

/** Payload to update an apartment, with the `YYYY-MM` month from which a changed total rent applies (null for the current month). */
export interface ApartmentUpdatePayload {
  apartment: ApartmentDetails
  rentEffectiveMonth: string | null
}
