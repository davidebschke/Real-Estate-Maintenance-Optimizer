import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { i18n } from '@/i18n'
import { useApartmentFormatting } from '@/composables/useApartmentFormatting'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Mounts the composable inside a real component instance, since it depends on useI18n injection. */
function mountComposable() {
  let composable!: ReturnType<typeof useApartmentFormatting>
  const Harness = defineComponent({
    setup() {
      composable = useApartmentFormatting()
      return () => h('div')
    },
  })
  mount(Harness, { global: { plugins: [i18n] } })
  return composable
}

describe('useApartmentFormatting', () => {
  it('labels the ground floor, upper floors and basement levels in German', () => {
    const { formatFloor } = mountComposable()

    expect(formatFloor(0)).toBe('Erdgeschoss')
    expect(formatFloor(2)).toBe('2. Stockwerk')
    expect(formatFloor(-1)).toBe('1. Untergeschoss')
  })

  it('labels the ground floor, upper floors and basement levels in English', () => {
    i18n.global.locale.value = 'en'
    const { formatFloor } = mountComposable()

    expect(formatFloor(0)).toBe('Ground floor')
    expect(formatFloor(2)).toBe('Floor 2')
    expect(formatFloor(-2)).toBe('Basement level 2')
  })

  it('formats the area with the locale decimal separator and at most two decimals', () => {
    const { formatArea } = mountComposable()

    expect(formatArea(64.5)).toBe('64,5 m²')
    expect(formatArea(64.555)).toBe('64,56 m²')

    i18n.global.locale.value = 'en'
    expect(formatArea(64.5)).toBe('64.5 m²')
  })

  it('formats money in euros for the active locale', () => {
    const { formatMoney } = mountComposable()

    expect(formatMoney(850)).toMatch(/^850,00\s€$/)

    i18n.global.locale.value = 'en'
    expect(formatMoney(850)).toBe('€850.00')
  })
})
