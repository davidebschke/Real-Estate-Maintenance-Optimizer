import type { ApartmentDetails } from '@/types/apartment'

/** Longest accepted first or last name of a tenant, in characters. */
export const TENANT_NAME_MAX_LENGTH = 50

/** Lowest accepted floor, a basement level. */
export const FLOOR_MIN = -3

/** Highest accepted floor. */
export const FLOOR_MAX = 100

/** Largest accepted living area in square meters. */
export const AREA_MAX = 99999.99

/** Largest accepted rent or service charge amount in euros. */
export const AMOUNT_MAX = 9999999.99

/** The apartment form fields as they are while being edited, empty ones being null. */
export interface ApartmentFieldValues {
  floor: number | null
  areaSquareMeters: number | null
  totalRent: number | null
  coldRent: number | null
  additionalCosts: number | null
}

/** Returns the field values as apartment details, or null while any of them is missing or outside its allowed range. */
export function toValidApartmentDetails(fields: ApartmentFieldValues): ApartmentDetails | null {
  const { floor, areaSquareMeters, totalRent, coldRent, additionalCosts } = fields
  if (floor === null || areaSquareMeters === null || totalRent === null || coldRent === null || additionalCosts === null) {
    return null
  }
  const isFloorValid = Number.isInteger(floor) && floor >= FLOOR_MIN && floor <= FLOOR_MAX
  const isAreaValid = areaSquareMeters > 0 && areaSquareMeters <= AREA_MAX
  const areAmountsValid = [totalRent, coldRent, additionalCosts].every((amount) => amount >= 0 && amount <= AMOUNT_MAX)
  return isFloorValid && isAreaValid && areAmountsValid
    ? { floor, areaSquareMeters, totalRent, coldRent, additionalCosts }
    : null
}
