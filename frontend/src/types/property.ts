/** A managed real-estate object ("Objekt"), selectable when creating an appointment and listed on the properties page. */
export interface Property {
  id: string
  name: string
  address: string
  icon: string
  latitude: number | null
  longitude: number | null
  /** Number of apartments of this property. */
  apartmentCount: number
  /** Number of tenants living in the apartments of this property. */
  tenantCount: number
  /** The month (`YYYY-MM`) from which the rent payment check applies, or null while it is not activated. */
  rentTrackingStart: string | null
}

/** Payload for creating a new property. */
export interface CreatePropertyPayload {
  name: string
  address: string
  latitude: number | null
  longitude: number | null
}
