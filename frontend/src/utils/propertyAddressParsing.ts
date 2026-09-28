/** The four address fields recombined by `PropertyFormDialog` into a property's stored `address` string. */
export interface ParsedPropertyAddress {
  street: string
  houseNumber: string
  addressSupplement: string
  postalCode: string
  city: string
}

/** Splits a stored "Straße Hausnummer[Zusatz], PLZ Ort" address back into its individual form fields, or null if it does not match that format. */
export function parsePropertyAddress(address: string): ParsedPropertyAddress | null {
  const [streetPart, cityPart] = address.split(', ')
  const streetMatch = streetPart?.match(/^(.*?)\s+(\d{1,5})([^\d\s]*)$/)
  const cityMatch = cityPart?.match(/^(\d{5})\s+(.+)$/)

  if (!streetMatch || !cityMatch) return null

  return {
    street: streetMatch[1]!,
    houseNumber: streetMatch[2]!,
    addressSupplement: streetMatch[3]!,
    postalCode: cityMatch[1]!,
    city: cityMatch[2]!,
  }
}
