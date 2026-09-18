import { describe, expect, it } from 'vitest'
import { useCardTilt } from '@/composables/useCardTilt'

/** Builds a minimal pointer event whose target reports a fixed 100x100 bounding box at the origin. */
function pointerEventAt(clientX: number, clientY: number): PointerEvent {
  const currentTarget = {
    getBoundingClientRect: () => ({ left: 0, top: 0, width: 100, height: 100 }),
  }
  return { clientX, clientY, currentTarget } as unknown as PointerEvent
}

describe('useCardTilt', () => {
  it('starts flat, with no tilt applied', () => {
    const { tiltStyle } = useCardTilt()
    expect(tiltStyle.value).toEqual({ '--tilt-rotate-x': '0deg', '--tilt-rotate-y': '0deg' })
  })

  it('tilts toward the top-left when the pointer is near that corner', () => {
    const { tiltStyle, onPointerMove } = useCardTilt()

    onPointerMove(pointerEventAt(10, 10))

    expect(tiltStyle.value['--tilt-rotate-x']).toMatch(/^[\d.]+deg$/)
    expect(tiltStyle.value['--tilt-rotate-y']).toMatch(/^-[\d.]+deg$/)
  })

  it('tilts the opposite way when the pointer moves to the bottom-right corner', () => {
    const { tiltStyle, onPointerMove } = useCardTilt()

    onPointerMove(pointerEventAt(90, 90))

    expect(tiltStyle.value['--tilt-rotate-x']).toMatch(/^-[\d.]+deg$/)
    expect(tiltStyle.value['--tilt-rotate-y']).toMatch(/^[\d.]+deg$/)
  })

  it('applies no tilt when the pointer is exactly centered', () => {
    const { tiltStyle, onPointerMove } = useCardTilt()

    onPointerMove(pointerEventAt(50, 50))

    expect(tiltStyle.value).toEqual({ '--tilt-rotate-x': '0.00deg', '--tilt-rotate-y': '0.00deg' })
  })

  it('resets to flat once the pointer leaves', () => {
    const { tiltStyle, onPointerMove, onPointerLeave } = useCardTilt()
    onPointerMove(pointerEventAt(10, 10))

    onPointerLeave()

    expect(tiltStyle.value).toEqual({ '--tilt-rotate-x': '0deg', '--tilt-rotate-y': '0deg' })
  })
})
