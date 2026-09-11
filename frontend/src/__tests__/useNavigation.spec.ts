import { afterEach, describe, it, expect } from 'vitest'
import { useNavigation } from '@/composables/useNavigation'

afterEach(() => {
  useNavigation().closePopup()
})

describe('useNavigation', () => {
  it('shares the same popup visibility state across every call', () => {
    const first = useNavigation()
    const second = useNavigation()

    expect(first.isPopupVisible).toBe(second.isPopupVisible)
  })

  it('opens the popup when a navigation entry is selected', () => {
    const { isPopupVisible, selectNavigationEntry } = useNavigation()

    expect(isPopupVisible.value).toBe(false)
    selectNavigationEntry('overview')

    expect(isPopupVisible.value).toBe(true)
  })

  it('closes the popup', () => {
    const { isPopupVisible, selectNavigationEntry, closePopup } = useNavigation()
    selectNavigationEntry('calendar')

    closePopup()

    expect(isPopupVisible.value).toBe(false)
  })
})
