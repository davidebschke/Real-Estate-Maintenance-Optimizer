const HOUSE_NUMBER_PATTERN = /^\d{1,5}$/
const POSTAL_CODE_PATTERN = /^\d{5}$/
/** Letters (incl. diacritics), digits, spaces, punctuation common to real German street/place names, and the typographic apostrophe/dash/non-breaking-space "smart punctuation" autocorrect commonly substitutes. */
const PLACE_NAME_PATTERN = /^[\p{L}\d .'’ /–-]+$/u

/** Returns whether a street or city is non-empty and contains only characters that occur in real German street/place names. */
export function isValidPlaceName(value: string): boolean {
  return PLACE_NAME_PATTERN.test(value.trim())
}

/** Returns whether a house number consists of one to five digits. */
export function isValidHouseNumber(value: string): boolean {
  return HOUSE_NUMBER_PATTERN.test(value.trim())
}

/** Returns whether a postal code consists of exactly five digits. */
export function isValidPostalCode(value: string): boolean {
  return POSTAL_CODE_PATTERN.test(value)
}
