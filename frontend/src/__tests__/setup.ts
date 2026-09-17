/** Polyfills `window.matchMedia`, which jsdom does not implement but PrimeVue's overlay components (e.g. Select) call on mount. */
if (typeof window.matchMedia !== 'function') {
  window.matchMedia = (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  })
}

/** Polyfills `ResizeObserver`, which jsdom does not implement but `AppointmentMap` uses to keep Leaflet sized correctly. */
if (typeof globalThis.ResizeObserver !== 'function') {
  globalThis.ResizeObserver = class ResizeObserver {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
  }
}
