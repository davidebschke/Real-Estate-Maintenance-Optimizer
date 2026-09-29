import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useMediaQuery } from '@/composables/useMediaQuery'

/** Stubs window.matchMedia to report the given initial match state and hands back its change listeners. */
function stubMatchMedia(initialMatches: boolean) {
  const listeners: Array<(event: MediaQueryListEvent) => void> = []
  const mediaQueryList = {
    matches: initialMatches,
    media: '',
    addEventListener: vi.fn<(type: string, listener: (event: MediaQueryListEvent) => void) => void>(
      (_type, listener) => {
        listeners.push(listener)
      },
    ),
    removeEventListener: vi.fn<
      (type: string, listener: (event: MediaQueryListEvent) => void) => void
    >((_type, listener) => {
      const index = listeners.indexOf(listener)
      if (index !== -1) listeners.splice(index, 1)
    }),
  }
  vi.stubGlobal(
    'matchMedia',
    vi.fn(() => mediaQueryList),
  )
  return {
    mediaQueryList,
    fireChange: (matches: boolean) => {
      mediaQueryList.matches = matches
      listeners.forEach((listener) => listener({ matches } as MediaQueryListEvent))
    },
  }
}

/** Mounts a minimal host component so the composable's lifecycle hooks run. */
function mountUseMediaQuery(query: string) {
  let result!: ReturnType<typeof useMediaQuery>
  const wrapper = mount(
    defineComponent({
      setup() {
        result = useMediaQuery(query)
        return () => h('div')
      },
    }),
  )
  return { wrapper, result }
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('useMediaQuery', () => {
  it('reflects the media query already matching once mounted', () => {
    stubMatchMedia(true)

    const { result } = mountUseMediaQuery('(max-width: 48rem)')

    expect(result.matches.value).toBe(true)
  })

  it('reflects the media query not matching once mounted', () => {
    stubMatchMedia(false)

    const { result } = mountUseMediaQuery('(max-width: 48rem)')

    expect(result.matches.value).toBe(false)
  })

  it('updates when the media query match state changes', () => {
    const { fireChange } = stubMatchMedia(false)
    const { result } = mountUseMediaQuery('(max-width: 48rem)')

    fireChange(true)

    expect(result.matches.value).toBe(true)
  })

  it('stops listening once the host component unmounts', () => {
    const { mediaQueryList } = stubMatchMedia(false)
    const { wrapper } = mountUseMediaQuery('(max-width: 48rem)')

    wrapper.unmount()

    expect(mediaQueryList.removeEventListener).toHaveBeenCalled()
  })
})
