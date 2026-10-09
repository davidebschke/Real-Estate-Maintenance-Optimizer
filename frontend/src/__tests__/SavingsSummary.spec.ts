import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import SavingsSummary from '@/components/statistics/SavingsSummary.vue'
import { createSavingsStatistics } from '@/__tests__/optimizationFixtures'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

function tileText(wrapper: ReturnType<typeof mount>, tile: string) {
  return {
    label: wrapper.find(`[data-tile="${tile}"] .savings-summary__label`).text(),
    value: wrapper.find(`[data-tile="${tile}"] .savings-summary__value`).text(),
  }
}

describe('SavingsSummary', () => {
  it('shows the saved kilometres, driving time and accepted proposals in total', () => {
    const wrapper = mount(SavingsSummary, {
      props: { statistics: createSavingsStatistics() },
      global: { plugins: [i18n] },
    })

    expect(tileText(wrapper, 'distance')).toEqual({ label: 'Gesparte Kilometer', value: '12,5 km' })
    expect(tileText(wrapper, 'duration')).toEqual({ label: 'Gesparte Fahrzeit', value: '30 Min' })
    expect(tileText(wrapper, 'accepted')).toEqual({ label: 'Übernommene Vorschläge', value: '3' })
  })

  it('formats the totals for the English locale', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(SavingsSummary, {
      props: { statistics: createSavingsStatistics({ totalSavedDistanceMeters: 1_234_500, totalSavedDurationSeconds: 5_400 }) },
      global: { plugins: [i18n] },
    })

    expect(tileText(wrapper, 'distance')).toEqual({ label: 'Kilometres saved', value: '1,234.5 km' })
    expect(tileText(wrapper, 'duration')).toEqual({ label: 'Driving time saved', value: '1 hr 30 min' })
  })
})
