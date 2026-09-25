import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { useAppointmentDragAndDrop } from '@/composables/useAppointmentDragAndDrop'
import { useAppointmentsStore } from '@/stores/appointments'
import type { Appointment } from '@/types/appointment'
import type { VueCalView } from '@/types/vue-cal'

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: 'a1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: 'property-1',
    propertyName: 'Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: '',
    category: 'maintenance',
    start: new Date(2026, 7, 10, 9, 0),
    end: new Date(2026, 7, 10, 10, 0),
    locked: false,
    recurring: false,
    recurrenceIntervalMonths: null,
    materials: [],
    history: [],
    travelDistanceKm: 12,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

/** Stubs `getBoundingClientRect` on an element with a fixed rectangle for deterministic geometry math. */
function stubRect(
  element: HTMLElement,
  rect: { top: number; height: number; left: number; width: number },
) {
  element.getBoundingClientRect = () =>
    ({
      ...rect,
      bottom: rect.top + rect.height,
      right: rect.left + rect.width,
      x: rect.left,
      y: rect.top,
      toJSON: () => rect,
    }) as DOMRect
}

/** Builds one or more vue-cal day cells (0-720px, i.e. one pixel per minute of the 07:00–19:00 grid) side by side, appended to `document.body`. */
function buildGridCells(dayCount: number): { grid: HTMLDivElement; cells: HTMLDivElement[] } {
  const grid = document.createElement('div')
  const cells: HTMLDivElement[] = []
  for (let i = 0; i < dayCount; i++) {
    const cell = document.createElement('div')
    cell.className = 'vuecal__cell'
    stubRect(cell, { top: 0, height: 720, left: i * 100, width: 100 })
    grid.appendChild(cell)
    cells.push(cell)
  }
  document.body.appendChild(grid)
  return { grid, cells }
}

/** Appends a draggable appointment card into the given cell, positioned to represent its own time span within the 07:00–19:00 grid. */
function buildCard(
  cell: HTMLElement,
  appointmentId: string,
  topOffsetMinutes: number,
  durationMinutes: number,
) {
  const card = document.createElement('div')
  card.dataset.appointmentId = appointmentId
  stubRect(card, { top: topOffsetMinutes, height: durationMinutes, left: 0, width: 100 })
  cell.appendChild(card)
  return card
}

/** Stubs `document.elementFromPoint`, which jsdom does not implement, to resolve to the given cell regardless of coordinates. */
function mockElementFromPoint(cell: Element) {
  document.elementFromPoint = vi
    .fn<(x: number, y: number) => Element | null>()
    .mockReturnValue(cell)
}

/** Builds a typed mock for the composable's `onDrop` callback. */
function mockOnDrop() {
  return vi.fn<(appointmentId: string, newStart: Date, durationMinutes: number) => void>()
}

/** Wires the composable's pointerdown handler onto the grid exactly as `AppCalendar.vue` does, and dispatches a full pointer sequence. */
function drag(
  grid: HTMLElement,
  handlePointerDown: (event: PointerEvent) => void,
  card: HTMLElement,
  from: { x: number; y: number },
  to: { x: number; y: number },
) {
  grid.addEventListener('pointerdown', handlePointerDown as EventListener)
  card.dispatchEvent(pointerEvent('pointerdown', from))
  window.dispatchEvent(pointerEvent('pointermove', to))
  window.dispatchEvent(pointerEvent('pointerup', to))
}

/** Builds a bubbling mouse event carrying the pointer-event fields this composable reads, sidestepping jsdom's limited PointerEvent support. */
function pointerEvent(type: string, position: { x: number; y: number }): PointerEvent {
  const event = new MouseEvent(type, {
    clientX: position.x,
    clientY: position.y,
    button: 0,
    bubbles: true,
    cancelable: true,
  })
  return Object.assign(event, { pointerId: 1, pointerType: 'mouse' }) as unknown as PointerEvent
}

describe('useAppointmentDragAndDrop', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers({ now: new Date(2026, 7, 1) })
  })

  afterEach(() => {
    vi.useRealTimers()
    document.body.innerHTML = ''
  })

  it('reschedules the appointment to the drop time, keeping the grab offset and snapping to 30 minutes', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ start: new Date(2026, 7, 10, 9, 0), end: new Date(2026, 7, 10, 10, 0) }),
    ]
    const { grid, cells } = buildGridCells(1)
    const card = buildCard(cells[0]!, 'a1', 120, 60)
    mockElementFromPoint(cells[0]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('day'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 10) }),
      onDrop,
    )

    drag(grid, handlePointerDown, card, { x: 0, y: 120 }, { x: 0, y: 300 })

    expect(onDrop).toHaveBeenCalledWith('a1', new Date(2026, 7, 10, 12, 0, 0, 0), 60)
  })

  it('maps the drop column to the matching day in the visible range', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ start: new Date(2026, 7, 10, 9, 0), end: new Date(2026, 7, 10, 10, 0) }),
    ]
    const { grid, cells } = buildGridCells(2)
    const card = buildCard(cells[0]!, 'a1', 120, 60)
    mockElementFromPoint(cells[1]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('week'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 16) }),
      onDrop,
    )

    drag(grid, handlePointerDown, card, { x: 0, y: 120 }, { x: 100, y: 120 })

    expect(onDrop).toHaveBeenCalledWith('a1', new Date(2026, 7, 11, 9, 0, 0, 0), 60)
  })

  it('does not start a drag for a locked appointment', () => {
    const store = useAppointmentsStore()
    store.appointments = [createAppointment({ locked: true })]
    const { grid, cells } = buildGridCells(1)
    const card = buildCard(cells[0]!, 'a1', 120, 60)
    mockElementFromPoint(cells[0]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown, preview } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('day'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 10) }),
      onDrop,
    )

    drag(grid, handlePointerDown, card, { x: 0, y: 120 }, { x: 0, y: 300 })

    expect(onDrop).not.toHaveBeenCalled()
    expect(preview.isDragging).toBe(false)
  })

  it('does not start a drag for a completed appointment', () => {
    const store = useAppointmentsStore()
    store.appointments = [createAppointment({ completed: true })]
    const { grid, cells } = buildGridCells(1)
    const card = buildCard(cells[0]!, 'a1', 120, 60)
    mockElementFromPoint(cells[0]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('day'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 10) }),
      onDrop,
    )

    drag(grid, handlePointerDown, card, { x: 0, y: 120 }, { x: 0, y: 300 })

    expect(onDrop).not.toHaveBeenCalled()
  })

  it('does not start a drag outside the day/week view', () => {
    const store = useAppointmentsStore()
    store.appointments = [createAppointment()]
    const { grid, cells } = buildGridCells(1)
    const card = buildCard(cells[0]!, 'a1', 120, 60)
    mockElementFromPoint(cells[0]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('month'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 10) }),
      onDrop,
    )

    drag(grid, handlePointerDown, card, { x: 0, y: 120 }, { x: 0, y: 300 })

    expect(onDrop).not.toHaveBeenCalled()
  })

  it('treats a small pointer movement as a click rather than a drag', () => {
    const store = useAppointmentsStore()
    store.appointments = [createAppointment()]
    const { grid, cells } = buildGridCells(1)
    const card = buildCard(cells[0]!, 'a1', 120, 60)
    mockElementFromPoint(cells[0]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown, preview } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('day'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 10) }),
      onDrop,
    )

    drag(grid, handlePointerDown, card, { x: 0, y: 120 }, { x: 0, y: 122 })

    expect(onDrop).not.toHaveBeenCalled()
    expect(preview.isDragging).toBe(false)
  })

  it('rejects a drop that resolves to a time in the past', () => {
    vi.setSystemTime(new Date(2026, 7, 10, 13, 0))
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ start: new Date(2026, 7, 10, 9, 0), end: new Date(2026, 7, 10, 10, 0) }),
    ]
    const { grid, cells } = buildGridCells(1)
    const card = buildCard(cells[0]!, 'a1', 120, 60)
    mockElementFromPoint(cells[0]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('day'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 10) }),
      onDrop,
    )

    drag(grid, handlePointerDown, card, { x: 0, y: 120 }, { x: 0, y: 200 })

    expect(onDrop).not.toHaveBeenCalled()
  })

  it('does not suppress a click on a different, non-draggable card after an earlier drag completed', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: 'a1' }),
      createAppointment({ id: 'a2', locked: true }),
    ]
    const { grid, cells } = buildGridCells(1)
    const draggedCard = buildCard(cells[0]!, 'a1', 120, 60)
    const lockedCard = buildCard(cells[0]!, 'a2', 300, 60)
    mockElementFromPoint(cells[0]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown, wasLastInteractionADrag } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('day'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 10) }),
      onDrop,
    )

    drag(grid, handlePointerDown, draggedCard, { x: 0, y: 120 }, { x: 0, y: 300 })
    expect(onDrop).toHaveBeenCalled()

    lockedCard.dispatchEvent(pointerEvent('pointerdown', { x: 0, y: 300 }))

    expect(wasLastInteractionADrag()).toBe(false)
  })

  it('ignores a second press on another card while a drag is already in progress', () => {
    const store = useAppointmentsStore()
    store.appointments = [createAppointment({ id: 'a1' }), createAppointment({ id: 'a2' })]
    const { grid, cells } = buildGridCells(1)
    const cardA = buildCard(cells[0]!, 'a1', 120, 60)
    const cardB = buildCard(cells[0]!, 'a2', 300, 60)
    mockElementFromPoint(cells[0]!)

    const onDrop = mockOnDrop()
    const { handlePointerDown } = useAppointmentDragAndDrop(
      ref(grid),
      ref<VueCalView>('day'),
      ref({ start: new Date(2026, 7, 10), end: new Date(2026, 7, 10) }),
      onDrop,
    )

    grid.addEventListener('pointerdown', handlePointerDown as EventListener)
    cardA.dispatchEvent(pointerEvent('pointerdown', { x: 0, y: 120 }))
    window.dispatchEvent(pointerEvent('pointermove', { x: 0, y: 300 }))
    cardB.dispatchEvent(pointerEvent('pointerdown', { x: 0, y: 300 }))
    window.dispatchEvent(pointerEvent('pointerup', { x: 0, y: 300 }))

    expect(onDrop).toHaveBeenCalledTimes(1)
    expect(onDrop).toHaveBeenCalledWith('a1', expect.any(Date), 60)
  })
})
