import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import DashboardCharts from '@/modules/dashboard/components/DashboardCharts.vue';
import type { ContractStatsSummary } from '@/shared/api/contracts';

// 收集每个图表实例的 setOption 调用，便于断言「统计数据 -> option」映射。
const setOptionCalls: any[] = [];

vi.mock('echarts', () => {
  const makeChart = () => ({
    setOption: vi.fn((opt: unknown) => setOptionCalls.push(opt)),
    resize: vi.fn(),
    dispose: vi.fn()
  });
  return {
    init: vi.fn(() => makeChart()),
    graphic: {
      LinearGradient: class {
        constructor(..._args: unknown[]) {}
      }
    }
  };
});

const stats: ContractStatsSummary = {
  total: 6,
  statusDistribution: [
    { status: 'DRAFT', count: 2 },
    { status: 'SIGNED', count: 4 }
  ],
  typeDistribution: [
    { type: '采购合同', count: 3 },
    { type: '服务合同', count: 3 }
  ],
  monthlyTrend: [
    { year: 2026, month: 5, count: 1 },
    { year: 2026, month: 6, count: 5 }
  ]
};

function optionWithSeriesType(type: string) {
  return setOptionCalls.find((opt) => opt?.series?.[0]?.type === type);
}

beforeEach(() => {
  setOptionCalls.length = 0;
});

afterEach(() => {
  document.body.innerHTML = '';
});

describe('DashboardCharts', () => {
  it('挂载后为三个图表分别 setOption', async () => {
    mount(DashboardCharts, { props: { statistics: stats } });
    await flushPromises();
    expect(optionWithSeriesType('pie')).toBeTruthy();
    expect(optionWithSeriesType('bar')).toBeTruthy();
    expect(optionWithSeriesType('line')).toBeTruthy();
  });

  it('状态分布饼图用中文标签映射数据', async () => {
    mount(DashboardCharts, { props: { statistics: stats } });
    await flushPromises();
    const pie = optionWithSeriesType('pie');
    expect(pie.series[0].data).toEqual([
      { name: '草稿', value: 2 },
      { name: '已签署', value: 4 }
    ]);
  });

  it('类型分布柱图取 type 作为 x 轴、count 作为数据', async () => {
    mount(DashboardCharts, { props: { statistics: stats } });
    await flushPromises();
    const bar = optionWithSeriesType('bar');
    expect(bar.xAxis.data).toEqual(['采购合同', '服务合同']);
    expect(bar.series[0].data).toEqual([3, 3]);
  });

  it('趋势折线图按 年-月 组织 x 轴', async () => {
    mount(DashboardCharts, { props: { statistics: stats } });
    await flushPromises();
    const line = optionWithSeriesType('line');
    expect(line.xAxis.data).toEqual(['2026-05', '2026-06']);
    expect(line.series[0].data).toEqual([1, 5]);
  });

  it('statistics 为 null 时不渲染图表', async () => {
    mount(DashboardCharts, { props: { statistics: null } });
    await flushPromises();
    expect(setOptionCalls).toHaveLength(0);
  });
});
