import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AppCalendarView,
  VueCalInstance,
  VueCalView,
  VueCalViewChangeEvent,
} from '@/types/vue-cal'
import { formatLocalizedDayNumber } from '@/utils/dateFormat'

/** Number of days, including today, the calendar's list view spans into the future — it always shows the same fixed forward-looking window regardless of navigation, unlike the grid views. */
export const LIST_VIEW_RANGE_DAYS = 365

/** Provides reusable state and controls for navigating the calendar grid: active view, visible range, previous/next/today. */
export function useCalendarNavigation(initialView: VueCalView = 'week') {
  const { locale } = useI18n()

  const vueCalRef = ref<VueCalInstance | null>(null)
  const activeView = ref<AppCalendarView>(initialView)
  /** vue-cal's own grid view, kept at its last real value while the app-level list view is active since vue-cal has no list mode of its own. */
  const gridView = ref<VueCalView>(initialView)
  const visibleRange = ref<{ start: Date; end: Date }>(computeInitialRange(gridView.value, new Date()))
  /** Fixed date range the list view covers, computed once for this navigation instance's lifetime since it does not follow previous/next/today navigation. */
  const listRange = computeListRange(new Date())

  watch(activeView, (view) => {
    if (view !== 'list') gridView.value = view
  })

  const rangeLabel = computed(() =>
    activeView.value === 'list'
      ? formatListRangeLabel(listRange.start, listRange.end, locale.value)
      : formatRangeLabel(visibleRange.value.start, visibleRange.value.end, gridView.value, locale.value),
  )

  /** Updates the visible date range whenever vue-cal reports a view or navigation change. */
  function handleViewChange(event: VueCalViewChangeEvent): void {
    visibleRange.value = { start: event.startDate, end: event.endDate }
  }

  /** Moves the calendar to the previous period for the active view. */
  function goToPrevious(): void {
    vueCalRef.value?.previous()
  }

  /** Moves the calendar to the next period for the active view. */
  function goToNext(): void {
    vueCalRef.value?.next()
  }

  /** Moves the calendar back to today without changing the active view. */
  function goToToday(): void {
    vueCalRef.value?.updateSelectedDate(new Date())
  }

  return {
    vueCalRef,
    activeView,
    gridView,
    visibleRange,
    listRange,
    rangeLabel,
    handleViewChange,
    goToPrevious,
    goToNext,
    goToToday,
  }
}

/** Computes the visible start/end range for the given view around a reference date, used before vue-cal reports its own range on navigation. */
function computeInitialRange(view: VueCalView, referenceDate: Date): { start: Date; end: Date } {
  switch (view) {
    case 'day':
      return { start: referenceDate, end: referenceDate }
    case 'week':
      return weekRange(referenceDate)
    case 'month':
      return monthRange(referenceDate)
    default:
      return yearRange(referenceDate)
  }
}

/** Returns the Monday-to-Sunday range of the week containing the given date. */
function weekRange(date: Date): { start: Date; end: Date } {
  const daysSinceMonday = (date.getDay() + 6) % 7
  const start = new Date(date.getFullYear(), date.getMonth(), date.getDate() - daysSinceMonday)
  const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 6)
  return { start, end }
}

/** Returns the first-to-last-day range of the month containing the given date. */
function monthRange(date: Date): { start: Date; end: Date } {
  return {
    start: new Date(date.getFullYear(), date.getMonth(), 1),
    end: new Date(date.getFullYear(), date.getMonth() + 1, 0),
  }
}

/** Returns the January 1 to December 31 range of the year containing the given date. */
function yearRange(date: Date): { start: Date; end: Date } {
  return { start: new Date(date.getFullYear(), 0, 1), end: new Date(date.getFullYear(), 11, 31) }
}

/** Returns the list view's fixed range: the calendar day of the given reference date through `LIST_VIEW_RANGE_DAYS - 1` days after it. */
function computeListRange(referenceDate: Date): { start: Date; end: Date } {
  const start = new Date(referenceDate.getFullYear(), referenceDate.getMonth(), referenceDate.getDate())
  const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + LIST_VIEW_RANGE_DAYS - 1)
  return { start, end }
}

/** Formats the list view's fixed date range as a localized title (e.g. "30. September 2026 – 29. September 2027"). */
function formatListRangeLabel(start: Date, end: Date, locale: string): string {
  const fullDate = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long', year: 'numeric' })
  return `${fullDate.format(start)} – ${fullDate.format(end)}`
}

/** Formats the visible date range as a localized title matching the active view (e.g. "10. – 14. August 2026"). */
function formatRangeLabel(start: Date, end: Date, view: VueCalView, locale: string): string {
  const dayMonth = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long' })
  const fullDate = new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
  const monthYear = new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' })
  const year = new Intl.DateTimeFormat(locale, { year: 'numeric' })

  switch (view) {
    case 'day':
      return fullDate.format(start)
    case 'week':
      if (start.getFullYear() !== end.getFullYear())
        return `${fullDate.format(start)} – ${fullDate.format(end)}`
      if (start.getMonth() !== end.getMonth())
        return `${dayMonth.format(start)} – ${fullDate.format(end)}`
      return `${formatLocalizedDayNumber(start, locale)} – ${fullDate.format(end)}`
    case 'month':
      return monthYear.format(start)
    default:
      return year.format(start)
  }
}
