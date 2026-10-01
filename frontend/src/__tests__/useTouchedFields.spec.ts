import { describe, expect, it } from 'vitest'
import { useTouchedFields } from '@/composables/useTouchedFields'

describe('useTouchedFields', () => {
  it('starts with every field untouched', () => {
    const { touched } = useTouchedFields(['name', 'city'])

    expect(touched).toEqual({ name: false, city: false })
  })

  it('marks only the given field as touched', () => {
    const { touched, markTouched } = useTouchedFields(['name', 'city'])

    markTouched('name')

    expect(touched).toEqual({ name: true, city: false })
  })

  it('resets every touched field', () => {
    const { touched, markTouched, resetTouched } = useTouchedFields(['name', 'city'])
    markTouched('name')
    markTouched('city')

    resetTouched()

    expect(touched).toEqual({ name: false, city: false })
  })
})
