import { describe, expect, it } from 'vitest'
import { toValidApartmentDetails, type ApartmentFieldValues } from '@/utils/apartmentFieldValidation'

/** Builds valid apartment field values, with overridable fields. */
function createFields(overrides: Partial<ApartmentFieldValues> = {}): ApartmentFieldValues {
  return { floor: 2, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200, ...overrides }
}

describe('toValidApartmentDetails', () => {
  it('returns the details for valid values', () => {
    expect(toValidApartmentDetails(createFields())).toEqual({
      floor: 2,
      areaSquareMeters: 64.5,
      totalRent: 850,
      coldRent: 650,
      additionalCosts: 200,
    })
  })

  it('returns null while any field is missing', () => {
    for (const field of ['floor', 'areaSquareMeters', 'totalRent', 'coldRent', 'additionalCosts'] as const) {
      expect(toValidApartmentDetails(createFields({ [field]: null }))).toBeNull()
    }
  })

  it('accepts the ground floor, the lowest basement and the highest floor but nothing beyond', () => {
    expect(toValidApartmentDetails(createFields({ floor: 0 }))).not.toBeNull()
    expect(toValidApartmentDetails(createFields({ floor: -3 }))).not.toBeNull()
    expect(toValidApartmentDetails(createFields({ floor: 100 }))).not.toBeNull()
    expect(toValidApartmentDetails(createFields({ floor: -4 }))).toBeNull()
    expect(toValidApartmentDetails(createFields({ floor: 101 }))).toBeNull()
  })

  it('rejects a floor with decimals', () => {
    expect(toValidApartmentDetails(createFields({ floor: 2.5 }))).toBeNull()
  })

  it('requires an area greater than zero and at most 99999.99', () => {
    expect(toValidApartmentDetails(createFields({ areaSquareMeters: 0 }))).toBeNull()
    expect(toValidApartmentDetails(createFields({ areaSquareMeters: 0.01 }))).not.toBeNull()
    expect(toValidApartmentDetails(createFields({ areaSquareMeters: 99999.99 }))).not.toBeNull()
    expect(toValidApartmentDetails(createFields({ areaSquareMeters: 100000 }))).toBeNull()
  })

  it('accepts amounts from zero up to 9999999.99 and nothing beyond', () => {
    expect(toValidApartmentDetails(createFields({ totalRent: 0, coldRent: 0, additionalCosts: 0 }))).not.toBeNull()
    expect(toValidApartmentDetails(createFields({ totalRent: 9999999.99 }))).not.toBeNull()
    expect(toValidApartmentDetails(createFields({ coldRent: 10000000 }))).toBeNull()
    expect(toValidApartmentDetails(createFields({ additionalCosts: -1 }))).toBeNull()
  })
})
