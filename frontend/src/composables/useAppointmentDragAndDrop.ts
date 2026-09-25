import { reactive, type Ref } from 'vue'
import { useAppointmentsStore } from '@/stores/appointments'
import type { VueCalView } from '@/types/vue-cal'

/** Minute-of-day the calendar's time grid starts at (07:00), matching `AppCalendar.vue`'s `time-from` prop. */
const TIME_FROM_MINUTES = 420
/** Minute-of-day the calendar's time grid ends at (19:00), matching `AppCalendar.vue`'s `time-to` prop. */
const TIME_TO_MINUTES = 1140
/** Rounds a dropped start time to the same granularity as the manual reschedule form's time options. */
const SNAP_MINUTES = 30
/** Pointer movement, in pixels, that must be exceeded before a press is treated as a drag rather than a click. */
const DRAG_THRESHOLD_PX = 6
/** CSS selector for one of vue-cal's day-column cell elements in the time grid. */
const VUECAL_CELL_SELECTOR = '.vuecal__cell'

export interface DragPreviewState {
  isDragging: boolean
  isValidDrop: boolean
  label: string
  pointerX: number
  pointerY: number
}

/** Reschedules an appointment by dragging its calendar card onto a new day/time cell, via Pointer Events so the same code path works with mouse, touch and pen input. */
export function useAppointmentDragAndDrop(
  gridElement: Ref<HTMLElement | null>,
  activeView: Ref<VueCalView>,
  visibleRange: Ref<{ start: Date; end: Date }>,
  onDrop: (appointmentId: string, newStart: Date, durationMinutes: number) => void | Promise<void>,
  onLockedDragAttempt: (appointmentId: string) => void,
) {
  const store = useAppointmentsStore()

  const preview = reactive<DragPreviewState>({
    isDragging: false,
    isValidDrop: false,
    label: '',
    pointerX: 0,
    pointerY: 0,
  })

  let sourceElement: HTMLElement | null = null
  let sourceAppointmentId: string | null = null
  let sourceDurationMinutes = 0
  let sourceOriginalStart: Date | null = null
  /** Minutes between the card's own top edge and the point the pointer grabbed it, so dragging keeps that same grab point under the pointer. */
  let grabOffsetMinutes = 0
  let startX = 0
  let startY = 0
  let pointerId: number | null = null
  let hasCrossedThreshold = false
  let pendingDropStart: Date | null = null
  let targetCell: Element | null = null
  /** The visible day-column cells, snapshotted once a drag starts so `pointermove` doesn't re-query the DOM on every event. */
  let dayCells: Element[] = []
  /** Whether the most recently completed pointer press on a card crossed the drag threshold, so the click vue-cal fires from that same press can be ignored. */
  let didJustDrag = false
  /** Whether the current press is tracked only to detect a drag attempt on a locked appointment, never to actually move it. */
  let isLockedPress = false

  /** Starts tracking a potential drag when a pointer goes down on a draggable appointment card. */
  function handlePointerDown(event: PointerEvent) {
    if (sourceElement) return
    didJustDrag = false
    if (event.button !== 0 && event.pointerType === 'mouse') return
    if (!isDraggableView(activeView.value)) return

    const card = (event.target as HTMLElement).closest<HTMLElement>('[data-appointment-id]')
    if (!card) return

    const appointmentId = card.dataset.appointmentId
    if (!appointmentId) return

    const appointment = store.appointments.find((candidate) => candidate.id === appointmentId)
    if (!appointment || appointment.completed) return

    if (appointment.locked) {
      trackLockedPress(card, appointmentId, event)
      return
    }

    const durationMinutes = (appointment.end.getTime() - appointment.start.getTime()) / 60000
    if (durationMinutes >= TIME_TO_MINUTES - TIME_FROM_MINUTES) return

    const cardRect = card.getBoundingClientRect()
    sourceElement = card
    sourceAppointmentId = appointmentId
    sourceOriginalStart = appointment.start
    sourceDurationMinutes = durationMinutes
    grabOffsetMinutes =
      cardRect.height > 0
        ? ((event.clientY - cardRect.top) / cardRect.height) * sourceDurationMinutes
        : 0
    startX = event.clientX
    startY = event.clientY
    pointerId = event.pointerId
    hasCrossedThreshold = false
    isLockedPress = false

    window.addEventListener('pointermove', handlePointerMove)
    window.addEventListener('pointerup', handlePointerUp)
    window.addEventListener('pointercancel', handlePointerCancel)
  }

  /** Tracks a press on a locked card only far enough to tell a real drag attempt apart from a plain click, so a blocked drag can still surface feedback. */
  function trackLockedPress(card: HTMLElement, appointmentId: string, event: PointerEvent) {
    sourceElement = card
    sourceAppointmentId = appointmentId
    startX = event.clientX
    startY = event.clientY
    pointerId = event.pointerId
    hasCrossedThreshold = false
    isLockedPress = true

    window.addEventListener('pointermove', handlePointerMove)
    window.addEventListener('pointerup', handlePointerUp)
    window.addEventListener('pointercancel', handlePointerCancel)
  }

  /** Tracks pointer movement, entering drag mode past a small threshold and updating the drop preview. */
  function handlePointerMove(event: PointerEvent) {
    if (!sourceElement || !sourceAppointmentId) return

    if (!hasCrossedThreshold) {
      const distance = Math.hypot(event.clientX - startX, event.clientY - startY)
      if (distance < DRAG_THRESHOLD_PX) return

      if (isLockedPress) {
        didJustDrag = true
        onLockedDragAttempt(sourceAppointmentId)
        resetDragState()
        return
      }
      enterDragMode(event)
    }

    updateDropPreview(event)
  }

  /** Switches from "pending click" into an active drag: captures the pointer and makes the source card pass through hit-testing. */
  function enterDragMode(event: PointerEvent) {
    hasCrossedThreshold = true
    didJustDrag = true
    preview.isDragging = true
    dayCells = gridElement.value
      ? Array.from(gridElement.value.querySelectorAll(VUECAL_CELL_SELECTOR))
      : []
    sourceElement?.setPointerCapture?.(pointerId ?? event.pointerId)
    sourceElement?.classList.add('is-dragging')
    if (sourceElement) sourceElement.style.pointerEvents = 'none'
  }

  /** Recomputes the drop target day/time under the pointer and updates the preview badge and cell highlight. */
  function updateDropPreview(event: PointerEvent) {
    preview.pointerX = event.clientX
    preview.pointerY = event.clientY

    const dropStart = resolveDropStart(event.clientX, event.clientY)
    setTargetCellHighlight(dropStart ? findCellAt(event.clientX, event.clientY) : null)

    pendingDropStart = dropStart
    preview.isValidDrop = Boolean(dropStart && !isInPast(dropStart))
    preview.label = dropStart ? formatPreviewLabel(dropStart) : ''
  }

  /** Finishes the drag, triggering the reschedule when the pointer was released over a valid target. */
  function handlePointerUp() {
    const appointmentId = sourceAppointmentId
    const dropStart = pendingDropStart
    const durationMinutes = sourceDurationMinutes
    const wasDragging = hasCrossedThreshold

    try {
      if (
        wasDragging &&
        appointmentId &&
        dropStart &&
        preview.isValidDrop &&
        dropStart.getTime() !== sourceOriginalStart?.getTime()
      ) {
        void onDrop(appointmentId, dropStart, durationMinutes)
      }
    } finally {
      resetDragState()
    }
  }

  /** Aborts an in-progress drag (e.g. the browser cancelled the pointer) without rescheduling anything. */
  function handlePointerCancel() {
    resetDragState()
  }

  /** Whether the card's most recent press was a drag, so the caller can ignore the click vue-cal still fires from that same press. */
  function wasLastInteractionADrag(): boolean {
    return didJustDrag
  }

  /** Clears all transient drag state and restores the source card's normal interaction behavior. */
  function resetDragState() {
    if (sourceElement) {
      sourceElement.classList.remove('is-dragging')
      sourceElement.style.pointerEvents = ''
      if (pointerId !== null) sourceElement.releasePointerCapture?.(pointerId)
    }
    setTargetCellHighlight(null)

    window.removeEventListener('pointermove', handlePointerMove)
    window.removeEventListener('pointerup', handlePointerUp)
    window.removeEventListener('pointercancel', handlePointerCancel)

    sourceElement = null
    sourceAppointmentId = null
    sourceOriginalStart = null
    pointerId = null
    hasCrossedThreshold = false
    isLockedPress = false
    pendingDropStart = null
    dayCells = []
    preview.isDragging = false
    preview.isValidDrop = false
    preview.label = ''
  }

  /** Returns the calendar date/time under the given pointer coordinates, or null when outside the time grid. */
  function resolveDropStart(clientX: number, clientY: number): Date | null {
    const cell = findCellAt(clientX, clientY)
    if (!cell) return null

    const columnIndex = dayCells.indexOf(cell)
    if (columnIndex === -1) return null

    const date = new Date(visibleRange.value.start)
    date.setDate(date.getDate() + columnIndex)

    const cellRect = cell.getBoundingClientRect()
    if (cellRect.height === 0) return null
    const offsetRatio = (clientY - cellRect.top) / cellRect.height
    const pointerMinutes = TIME_FROM_MINUTES + offsetRatio * (TIME_TO_MINUTES - TIME_FROM_MINUTES)
    const rawMinutes = pointerMinutes - grabOffsetMinutes
    const snappedMinutes = Math.round(rawMinutes / SNAP_MINUTES) * SNAP_MINUTES
    const clampedMinutes = Math.min(
      Math.max(snappedMinutes, TIME_FROM_MINUTES),
      TIME_TO_MINUTES - sourceDurationMinutes,
    )

    date.setHours(0, clampedMinutes, 0, 0)
    return date
  }

  /** Finds the vue-cal day cell under the given coordinates, ignoring the dragged card itself. */
  function findCellAt(clientX: number, clientY: number): Element | null {
    const elementUnderPointer = document.elementFromPoint(clientX, clientY)
    return elementUnderPointer?.closest(VUECAL_CELL_SELECTOR) ?? null
  }

  /** Toggles the drop-target highlight class onto the currently hovered cell. */
  function setTargetCellHighlight(cell: Element | null) {
    if (targetCell === cell) return
    targetCell?.classList.remove('app-calendar__cell--drop-target')
    cell?.classList.add('app-calendar__cell--drop-target')
    targetCell = cell
  }

  /** Formats the pending drop time as a short, locale-aware label for the drag preview badge. */
  function formatPreviewLabel(date: Date): string {
    return new Intl.DateTimeFormat(undefined, {
      weekday: 'short',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date)
  }

  /** Rejects a drop target that lies before the current moment, matching the manual form's upcoming-days-only choices. */
  function isInPast(date: Date): boolean {
    return date.getTime() < Date.now()
  }

  /** Whether the active calendar view has an hourly time grid precise enough for drag-and-drop rescheduling. */
  function isDraggableView(view: VueCalView): boolean {
    return view === 'day' || view === 'week'
  }

  return { preview, handlePointerDown, wasLastInteractionADrag }
}
