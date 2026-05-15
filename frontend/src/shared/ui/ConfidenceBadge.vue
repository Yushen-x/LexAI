<template>
  <div v-if="value !== null && value !== undefined" class="conf-badge" :class="level">
    <span class="conf-icon">●</span>
    <span class="conf-label">AI 自评可信度</span>
    <div class="conf-bar">
      <div class="conf-fill" :style="{ width: percent + '%' }"></div>
    </div>
    <span class="conf-num">{{ percent }}%</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  value?: number | null;
}
const props = defineProps<Props>();

const percent = computed(() => {
  if (props.value === null || props.value === undefined) return 0;
  const v = props.value;
  const ratio = v > 1 ? v / 100 : v;
  return Math.max(0, Math.min(100, Math.round(ratio * 100)));
});

const level = computed(() => {
  const p = percent.value;
  if (p >= 80) return 'high';
  if (p >= 60) return 'mid';
  return 'low';
});
</script>

<style scoped>
.conf-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: 0.78rem;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #334155;
}
.conf-badge.high { background: #ecfdf5; border-color: #86efac; color: #047857; }
.conf-badge.mid { background: #fef3c7; border-color: #fcd34d; color: #92400e; }
.conf-badge.low { background: #fee2e2; border-color: #fca5a5; color: #b91c1c; }

.conf-icon { font-size: 0.55rem; line-height: 1; }
.conf-label { font-weight: 600; }
.conf-bar {
  width: 80px;
  height: 5px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.25);
  overflow: hidden;
}
.conf-fill {
  height: 100%;
  background: currentColor;
  border-radius: 999px;
  transition: width 0.4s;
}
.conf-num { font-variant-numeric: tabular-nums; font-weight: 600; }
</style>
