import { createI18n } from 'vue-i18n'
import headerDe from '@/locales/de/header.json'
import headerEn from '@/locales/en/header.json'
import footerDe from '@/locales/de/footer.json'
import footerEn from '@/locales/en/footer.json'
import overviewDe from '@/locales/de/overview.json'
import overviewEn from '@/locales/en/overview.json'
import calendarDe from '@/locales/de/calendar.json'
import calendarEn from '@/locales/en/calendar.json'
import statisticsDe from '@/locales/de/statistics.json'
import statisticsEn from '@/locales/en/statistics.json'
import propertiesDe from '@/locales/de/properties.json'
import propertiesEn from '@/locales/en/properties.json'

/** Merges all namespaced message files into one locale bundle. */
const messages = {
  de: { ...headerDe, ...footerDe, ...overviewDe, ...calendarDe, ...statisticsDe, ...propertiesDe },
  en: { ...headerEn, ...footerEn, ...overviewEn, ...calendarEn, ...statisticsEn, ...propertiesEn },
}

/** Configures vue-i18n with German as default and English as fallback locale. */
export const i18n = createI18n({
  legacy: false,
  locale: 'de',
  fallbackLocale: 'en',
  messages,
})
