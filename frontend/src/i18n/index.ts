import { createI18n } from 'vue-i18n'
import headerDe from '@/locales/de/header.json'
import headerEn from '@/locales/en/header.json'
import footerDe from '@/locales/de/footer.json'
import footerEn from '@/locales/en/footer.json'

/** Merges all namespaced message files into one locale bundle. */
const messages = {
  de: { ...headerDe, ...footerDe },
  en: { ...headerEn, ...footerEn },
}

/** Configures vue-i18n with German as default and English as fallback locale. */
export const i18n = createI18n({
  legacy: false,
  locale: 'de',
  fallbackLocale: 'en',
  messages,
})
