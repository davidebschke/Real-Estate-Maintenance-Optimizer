declare module 'vue-cal' {
  import type { DefineComponent } from 'vue'
  import type { VueCalInstance, VueCalProps } from '@/types/vue-cal'

  const VueCal: DefineComponent<VueCalProps, VueCalInstance>
  export default VueCal
}

declare module 'vue-cal/dist/i18n/*.es.js' {
  const locale: Record<string, unknown>
  export default locale
}
