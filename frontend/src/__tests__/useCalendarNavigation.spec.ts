import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { i18n } from '@/i18n'
import { useCalendarNavigation } from '@/composables/useCalendarNavigation'
import type { VueCalView, VueCalViewChangeEvent } from '@/types/vue-cal'

/** Runs `useCalendarNavigation` inside a mounted host component so `useI18n` has a valid setup context. */
function mountNavigation(initialView: VueCalView = 'week') {
  let navigation!: ReturnType<typeof useCalendarNavigation>
  const HostComponent = defineComponent({
    setup() {
      navigation = useCalendarNavigation(initialView)
      return () => h('div')
    },
  })

  mount(HostComponent, { global: { plugins: [i18n] } })
  return navigation
}

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('useCalendarNavigation', () => {
  it('defaults the active view to the given initial view', () => {
    const navigation = mountNavigation('month')
    expect(navigation.activeView.value).toBe('month')
  })

  it('formats a same-month week range in German, matching the calendar toolbar title', () => {
    const navigation = mountNavigation('week')
    const event: VueCalViewChangeEvent = {
      view: 'week',
      startDate: new Date(2026, 7, 10),
      endDate: new Date(2026, 7, 14),
    }

    navigation.handleViewChange(event)

    expect(navigation.rangeLabel.value).toBe('10. – 14. August 2026')
  })

  it('formats a cross-month week range in German', () => {
    const navigation = mountNavigation('week')

    navigation.handleViewChange({
      view: 'week',
      startDate: new Date(2026, 6, 28),
      endDate: new Date(2026, 7, 3),
    })

    expect(navigation.rangeLabel.value).toBe('28. Juli – 3. August 2026')
  })

  it('formats the range in English when the app locale is switched', () => {
    i18n.global.locale.value = 'en'
    const navigation = mountNavigation('week')

    navigation.handleViewChange({
      view: 'week',
      startDate: new Date(2026, 7, 10),
      endDate: new Date(2026, 7, 14),
    })

    expect(navigation.rangeLabel.value).toBe('10 – August 14, 2026')
  })

  it('formats a single day, a month and a year for the respective views', () => {
    const dayNav = mountNavigation('day')
    dayNav.handleViewChange({
      view: 'day',
      startDate: new Date(2026, 7, 10),
      endDate: new Date(2026, 7, 10),
    })
    expect(dayNav.rangeLabel.value).toBe('10. August 2026')

    const monthNav = mountNavigation('month')
    monthNav.handleViewChange({
      view: 'month',
      startDate: new Date(2026, 7, 1),
      endDate: new Date(2026, 7, 31),
    })
    expect(monthNav.rangeLabel.value).toBe('August 2026')

    const yearNav = mountNavigation('year')
    yearNav.handleViewChange({
      view: 'year',
      startDate: new Date(2026, 0, 1),
      endDate: new Date(2026, 11, 31),
    })
    expect(yearNav.rangeLabel.value).toBe('2026')
  })

  it('delegates previous/next/today to the bound vue-cal instance', () => {
    const navigation = mountNavigation('week')
    const previous = vi.fn<() => void>()
    const next = vi.fn<() => void>()
    const switchView = vi.fn<(view: VueCalView) => void>()
    const updateSelectedDate = vi.fn<(date: Date) => void>()
    navigation.vueCalRef.value = { previous, next, switchView, updateSelectedDate }

    navigation.goToPrevious()
    navigation.goToNext()
    navigation.goToToday()

    expect(previous).toHaveBeenCalledOnce()
    expect(next).toHaveBeenCalledOnce()
    expect(updateSelectedDate).toHaveBeenCalledOnce()
  })

  it('does not throw when navigating before the vue-cal instance is bound', () => {
    const navigation = mountNavigation('week')

    expect(() => {
      navigation.goToPrevious()
      navigation.goToNext()
      navigation.goToToday()
    }).not.toThrow()
  })
})
