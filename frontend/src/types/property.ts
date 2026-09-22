/** A managed real-estate object ("Objekt"), selectable when creating an appointment and listed on the properties page. */
export interface Property {
  id: string
  name: string
  address: string
  icon: string
  latitude: number | null
  longitude: number | null
}

/** Payload for creating a new property. */
export interface CreatePropertyPayload {
  name: string
  address: string
  latitude: number | null
  longitude: number | null
}
