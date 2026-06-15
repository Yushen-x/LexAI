<template>
  <div class="charts-grid">
    <div class="card chart-card">
      <div class="card-header pb-3 border-b mb-3">
        <h3 class="card-title">合同状态分布</h3>
      </div>
      <div ref="statusChartRef" class="chart-box"></div>
    </div>

    <div class="card chart-card">
      <div class="card-header pb-3 border-b mb-3">
        <h3 class="card-title">合同类型分布</h3>
      </div>
      <div ref="typeChartRef" class="chart-box"></div>
    </div>

    <div class="card chart-card chart-card--wide">
      <div class="card-header pb-3 border-b mb-3">
        <h3 class="card-title">近 6 个月新建趋势</h3>
      </div>
      <div ref="trendChartRef" class="chart-box chart-box--tall"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { ContractStatsSummary } from '@/shared/api/contracts';

const props = defineProps<{
  statistics: ContractStatsSummary | null;
}>();

const statusChartRef = ref<HTMLDivElement | null>(null);
const typeChartRef = ref<HTMLDivElement | null>(null);
const trendChartRef = ref<HTMLDivElement | null>(null);

let statusChart: echarts.ECharts | null = null;
let typeChart: echarts.ECharts | null = null;
let trendChart: echarts.ECharts | null = null;

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  UNDER_REVIEW: '审查中',
  SIGNED: '已签署',
  IN_PROGRESS: '执行中',
  COMPLETED: '已完成',
  TERMINATED: '已终止'
};

const STATUS_COLORS = ['#6366f1', '#f59e0b', '#10b981', '#3b82f6', '#8b5cf6', '#ef4444'];

function initCharts(): void {
  if (statusChartRef.value) statusChart = echarts.init(statusChartRef.value);
  if (typeChartRef.value) typeChart = echarts.init(typeChartRef.value);
  if (trendChartRef.value) trendChart = echarts.init(trendChartRef.value);
  renderCharts();
}

function renderCharts(): void {
  const stats = props.statistics;
  if (!stats) return;

  statusChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    color: STATUS_COLORS,
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{c} ({d}%)' },
      data: stats.statusDistribution.map((item) => ({
        name: STATUS_LABELS[item.status] ?? item.status,
        value: item.count
      }))
    }]
  });

  typeChart?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 24, bottom: 48 },
    xAxis: {
      type: 'category',
      data: stats.typeDistribution.map((item) => item.type),
      axisLabel: { rotate: 25, interval: 0, fontSize: 11 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: stats.typeDistribution.map((item) => item.count),
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#6366f1' },
          { offset: 1, color: '#a5b4fc' }
        ])
      },
      barMaxWidth: 42
    }]
  });

  const trend = stats.monthlyTrend.slice(-6);
  trendChart?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 24, bottom: 32 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.map((item) => `${item.year}-${String(item.month).padStart(2, '0')}`)
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'line',
      smooth: true,
      symbolSize: 8,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(99, 102, 241, 0.35)' },
          { offset: 1, color: 'rgba(99, 102, 241, 0.02)' }
        ])
      },
      lineStyle: { width: 3, color: '#6366f1' },
      itemStyle: { color: '#6366f1' },
      data: trend.map((item) => item.count)
    }]
  });
}

function handleResize(): void {
  statusChart?.resize();
  typeChart?.resize();
  trendChart?.resize();
}

watch(
  () => props.statistics,
  () => renderCharts(),
  { deep: true }
);

onMounted(() => {
  initCharts();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  statusChart?.dispose();
  typeChart?.dispose();
  trendChart?.dispose();
});
</script>

<style scoped>
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
}

.chart-card {
  padding: 1rem 1rem 0.5rem;
}

.chart-card--wide {
  grid-column: 1 / -1;
}

.chart-box {
  width: 100%;
  height: 280px;
}

.chart-box--tall {
  height: 300px;
}

.card-title {
  margin: 0;
  font-size: 1rem;
}

.border-b {
  border-bottom: 1px solid var(--border-light);
}

.pb-3 {
  padding-bottom: 0.75rem;
}

.mb-3 {
  margin-bottom: 0.75rem;
}

@media (max-width: 900px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
