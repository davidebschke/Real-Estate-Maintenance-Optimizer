/** Single view mode supported by the vue-cal grid. */
export type VueCalView = 'years' | 'year' | 'month' | 'week' | 'day'

/** Payload emitted by vue-cal whenever the active view or its visible date range changes. */
export interface VueCalViewChangeEvent {
  view: VueCalView
  startDate: Date
  endDate: Date
}

/** Heading metadata passed to the `weekday-heading` scoped slot for one visible day column; `date` is only set in the day and week views. */
export interface VueCalWeekdayHeading {
  date?: Date
  label: string
}

/** Event data passed to the `event` scoped slot, with start/end already parsed into `Date` objects. */
export interface VueCalSlotEvent {
  appointmentId: string
  start: Date
  end: Date
  title: string
  content: string
  class: string
}

/** Event object in the shape the `events` prop expects, with start/end as `YYYY-MM-DD HH:mm` strings. */
export interface VueCalEventInput {
  appointmentId: string
  start: string
  end: string
  title: string
  content: string
  class: string
}

/** Payload vue-cal passes to the `event-click` listener when an event is clicked. */
export interface VueCalEventClickEvent {
  appointmentId: string
}

/** Props accepted by the vue-cal component that this application actually uses. */
export interface VueCalProps {
  activeView?: VueCalView
  disableViews?: VueCalView[]
  events?: VueCalEventInput[]
  locale?: Record<string, unknown>
  timeFrom?: number
  timeTo?: number
  timeStep?: number
  hideTitleBar?: boolean
  hideViewSelector?: boolean
  hideWeekdays?: number[]
  onEventClick?: (event: VueCalEventClickEvent, domEvent: Event) => void
}

/** Public instance API exposed by vue-cal through a template ref. */
export interface VueCalInstance {
  previous: () => void
  next: () => void
  switchView: (view: VueCalView) => void
  updateSelectedDate: (date: Date) => void
}
