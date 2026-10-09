import { useI18n } from 'vue-i18n'
import { useLocale } from '@/composables/useLocale'

/** Provides localized labels for the floor, area and rent figures of an apartment. */
export function useApartmentFormatting() {
  const { t } = useI18n()
  const { currentLocale } = useLocale()

  /** Formats a floor number as "Erdgeschoss", "{n}. Stockwerk" or "{n}. Untergeschoss" (or their English equivalents). */
  function formatFloor(floor: number): string {
    if (floor === 0) return t('tenants.apartment.floorGround')
    if (floor > 0) return t('tenants.apartment.floorUpper', { level: floor })
    return t('tenants.apartment.floorBasement', { level: Math.abs(floor) })
  }

  /** Formats an area in square meters with at most two decimal places. */
  function formatArea(areaSquareMeters: number): string {
    return `${new Intl.NumberFormat(currentLocale.value, { maximumFractionDigits: 2 }).format(areaSquareMeters)} m²`
  }

  /** Formats an amount of money in euros. */
  function formatMoney(amount: number): string {
    return new Intl.NumberFormat(currentLocale.value, { style: 'currency', currency: 'EUR' }).format(amount)
  }

  return { formatFloor, formatArea, formatMoney }
}
