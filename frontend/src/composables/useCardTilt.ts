import { ref } from 'vue'

const MAX_TILT_DEGREES = 6

/** Tracks a pointer-following 3D tilt as CSS custom properties, reusable across any card-like element. */
export function useCardTilt() {
  const tiltStyle = ref({ '--tilt-rotate-x': '0deg', '--tilt-rotate-y': '0deg' })

  /** Updates the tilt angles from the pointer position relative to the hovered element's bounds. */
  function onPointerMove(event: PointerEvent): void {
    const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
    const offsetX = (event.clientX - bounds.left) / bounds.width - 0.5
    const offsetY = (event.clientY - bounds.top) / bounds.height - 0.5
    tiltStyle.value = {
      '--tilt-rotate-x': `${(-offsetY * MAX_TILT_DEGREES).toFixed(2)}deg`,
      '--tilt-rotate-y': `${(offsetX * MAX_TILT_DEGREES).toFixed(2)}deg`,
    }
  }

  /** Resets the tilt back to flat once the pointer leaves the element. */
  function onPointerLeave(): void {
    tiltStyle.value = { '--tilt-rotate-x': '0deg', '--tilt-rotate-y': '0deg' }
  }

  return { tiltStyle, onPointerMove, onPointerLeave }
}
