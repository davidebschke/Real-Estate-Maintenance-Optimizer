import { onBeforeUnmount, onMounted, ref } from 'vue'

/** Tracks whether the given CSS media query currently matches, reusable for any viewport-based UI branch. */
export function useMediaQuery(query: string) {
  const matches = ref(window.matchMedia(query).matches)
  let mediaQueryList: MediaQueryList | null = null

  /** Updates the reactive match state whenever the media query's match status changes. */
  function handleChange(event: MediaQueryListEvent): void {
    matches.value = event.matches
  }

  onMounted(() => {
    mediaQueryList = window.matchMedia(query)
    matches.value = mediaQueryList.matches
    mediaQueryList.addEventListener('change', handleChange)
  })

  onBeforeUnmount(() => {
    mediaQueryList?.removeEventListener('change', handleChange)
  })

  return { matches }
}
