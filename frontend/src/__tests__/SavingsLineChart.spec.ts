import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { i18n } from '@/i18n'
import SavingsLineChart from '@/components/statistics/SavingsLineChart.vue'
import { createSavingsStatistics } from '@/__tests__/optimizationFixtures'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

/** Stands in for PrimeVue's Chart, which draws on a canvas jsdom cannot render, exposing what it was given. */
const ChartStub = defineComponent({
  name: 'ChartStub',
  props: { type: String, data: Object, options: Object },
  template: '<div class="chart-stub"></div>',
})

function mountChart() {
  return mount(SavingsLineChart, {
    props: { statistics: createSavingsStatistics() },
    global: { plugins: [i18n], stubs: { Chart: ChartStub } },
  })
}

describe('SavingsLineChart', () => {
  it('draws a line chart of the cumulative savings with a caption', () => {
    const wrapper = mountChart()
    const chart = wrapper.findComponent(ChartStub)

    expect(wrapper.find('.savings-line-chart__caption').text()).toBe('Verlauf der Einsparungen')
    expect(chart.props('type')).toBe('line')
    expect(chart.props('data')).toMatchObject({
      labels: ['28.09.', '05.10.'],
      datasets: [{ data: [10, 12.5] }, { data: [0.3, 0.5] }],
    })
    expect(chart.props('options')).toMatchObject({ responsive: true, maintainAspectRatio: false })
  })

  it('updates the chart when the statistics change', async () => {
    const wrapper = mountChart()

    await wrapper.setProps({ statistics: createSavingsStatistics({ periods: [] }) })

    expect(wrapper.findComponent(ChartStub).props('data')).toMatchObject({ labels: [] })
  })
})
