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

/** An apartment of a property together with the tenants living in it. */
export interface Apartment extends ApartmentDetails {
  id: string
  propertyId: string
  tenants: Tenant[]
}

/** The personal data of a tenant. */
export type TenantDetails = Pick<Tenant, 'firstName' | 'lastName'>

/** Payload combining the data of an apartment with the data of one tenant, used to create an apartment and to edit a tenant. */
export interface ApartmentWithTenantPayload {
  apartment: ApartmentDetails
  tenant: TenantDetails
}
