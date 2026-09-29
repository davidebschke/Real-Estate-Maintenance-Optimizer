/** Returns up to two uppercase initials of a display name, e.g. "DE" for "David Ebschke", "DA" for "Demo-Account" or "D" for "Demo". */
export function toInitials(displayName: string): string {
  const words = displayName.trim().split(/[\s-]+/).filter(Boolean)
  if (words.length === 0) return ''
  const first = words[0]!.charAt(0)
  const last = words.length > 1 ? words[words.length - 1]!.charAt(0) : ''
  return (first + last).toUpperCase()
}
