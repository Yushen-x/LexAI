<template>
  <span class="cited-text">
    <template v-for="(seg, idx) in segments" :key="idx">
      <span v-if="seg.type === 'text'">{{ seg.text }}</span>
      <span
        v-else
        class="cite-chip"
        :class="seg.kind"
        :title="tooltipFor(seg.kind, seg.index)"
        @click="emitCite(seg)"
      >{{ seg.label }}</span>
    </template>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { RetrievalContext } from '@/shared/types/legal';

interface Props {
  text: string;
  context?: RetrievalContext;
}
const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'cite-click', seg: { kind: string; index: number; label: string }): void;
}>();

function emitCite(seg: Seg) {
  if (seg.type === 'cite' && seg.kind && seg.index !== undefined && seg.label !== undefined) {
    emit('cite-click', { kind: seg.kind, index: seg.index, label: seg.label });
  }
}

interface Seg {
  type: 'text' | 'cite';
  text?: string;
  kind?: string;
  index?: number;
  label?: string;
}

const segments = computed<Seg[]>(() => {
  const out: Seg[] = [];
  if (!props.text) return out;
  const re = /\[([LCKlck])(\d+)\]/g;
  let last = 0;
  let m: RegExpExecArray | null;
  while ((m = re.exec(props.text)) !== null) {
    if (m.index > last) {
      out.push({ type: 'text', text: props.text.slice(last, m.index) });
    }
    const kindRaw = m[1].toUpperCase();
    const kind = kindRaw === 'L' ? 'law' : kindRaw === 'C' ? 'case' : 'kb';
    out.push({
      type: 'cite',
      kind,
      index: parseInt(m[2], 10),
      label: m[0],
    });
    last = m.index + m[0].length;
  }
  if (last < props.text.length) {
    out.push({ type: 'text', text: props.text.slice(last) });
  }
  return out;
});

function tooltipFor(kind?: string, idx?: number) {
  if (!props.context || !kind || !idx) return '';
  const arr = kind === 'law'
    ? props.context.laws
    : kind === 'case'
      ? props.context.cases
      : props.context.knowledge;
  const item = arr?.[idx - 1];
  if (!item) return `${kind.toUpperCase()}${idx}（未找到对应来源）`;
  const truncated = item.length > 200 ? item.slice(0, 200) + '...' : item;
  return truncated;
}
</script>

<style scoped>
.cite-chip {
  display: inline-block;
  padding: 1px 6px;
  margin: 0 2px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: help;
  vertical-align: middle;
  line-height: 1.4;
}
.cite-chip.law { background: #e0f2fe; color: #0369a1; border: 1px solid #7dd3fc; }
.cite-chip.case { background: #fef3c7; color: #92400e; border: 1px solid #fcd34d; }
.cite-chip.kb { background: #ede9fe; color: #6d28d9; border: 1px solid #c4b5fd; }
</style>
