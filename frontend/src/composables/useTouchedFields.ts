import { reactive } from 'vue'

/** Tracks which form fields have been left (blurred) at least once, so their required-field hints may be shown. */
export function useTouchedFields<FieldName extends string>(fieldNames: readonly FieldName[]) {
  const touched = reactive(
    Object.fromEntries(fieldNames.map((fieldName) => [fieldName, false])),
  ) as Record<FieldName, boolean>

  /** Marks the given field as touched. */
  function markTouched(fieldName: FieldName): void {
    touched[fieldName] = true
  }

  /** Marks every field as untouched again. */
  function resetTouched(): void {
    fieldNames.forEach((fieldName) => {
      touched[fieldName] = false
    })
  }

  return { touched, markTouched, resetTouched }
}
