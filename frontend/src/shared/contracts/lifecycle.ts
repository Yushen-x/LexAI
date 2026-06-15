import type { ContractItem, ContractReviewDecision, ContractStatus } from '@/shared/types/contracts';

export type StatusTone = 'success' | 'warning' | 'danger' | 'muted';

/** 合同状态的下拉选项 / 文案单一来源。 */
export const CONTRACT_STATUS_OPTIONS: { value: ContractStatus; label: string }[] = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'UNDER_REVIEW', label: '审查中' },
  { value: 'SIGNED', label: '已签署' },
  { value: 'IN_PROGRESS', label: '执行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'TERMINATED', label: '已终止' }
];

const STATUS_LABEL = new Map<string, string>(
  CONTRACT_STATUS_OPTIONS.map((o) => [o.value, o.label])
);

/** 状态的中文标签；未知状态原样返回，便于排查后端新增的枚举。 */
export function statusLabel(status: string): string {
  return STATUS_LABEL.get(status) ?? status;
}

/** 状态对应的语义色调，用于状态点 / 文字颜色。 */
export function statusTone(status: ContractStatus): StatusTone {
  switch (status) {
    case 'DRAFT':
      return 'muted';
    case 'UNDER_REVIEW':
      return 'warning';
    case 'SIGNED':
    case 'IN_PROGRESS':
    case 'COMPLETED':
      return 'success';
    case 'TERMINATED':
      return 'danger';
    default:
      return 'muted';
  }
}

/** 人工复核决策标签。未识别的决策按「待人工确认」处理。 */
export function reviewDecisionLabel(decision: string): string {
  switch (decision) {
    case 'APPROVED':
      return '审查通过';
    case 'NEEDS_REVISION':
      return '退回修改';
    case 'PENDING_CONFIRMATION':
    default:
      return '待人工确认';
  }
}

/** 生命周期主线顺序（不含终止态）。 */
export const LIFECYCLE_ORDER: ContractStatus[] = [
  'DRAFT',
  'UNDER_REVIEW',
  'SIGNED',
  'IN_PROGRESS',
  'COMPLETED'
];

/** stage 是否排在 current 之前（用于进度条「已完成」高亮）。 */
export function isStageBefore(stage: ContractStatus, current: ContractStatus): boolean {
  const s = LIFECYCLE_ORDER.indexOf(stage);
  const c = LIFECYCLE_ORDER.indexOf(current);
  if (s < 0 || c < 0) return false;
  return s < c;
}

export interface NextStatusAction {
  label: string;
  target: ContractStatus;
  tone: 'success' | 'danger';
}

/**
 * 给定合同，返回当前状态下允许推进到的下一步操作（纯数据，不含 UI 处理函数）。
 * 业务规则：
 * - DRAFT → 提交审查（UNDER_REVIEW）
 * - UNDER_REVIEW 且 AI 审查通过 → 标记已签署（SIGNED）
 * - SIGNED → 开始履约（IN_PROGRESS）
 * - IN_PROGRESS → 标记完成（COMPLETED）
 * - 非终态/非完成态 → 允许终止（TERMINATED）
 */
export function nextStatusActions(contract: ContractItem): NextStatusAction[] {
  const actions: NextStatusAction[] = [];
  const s = contract.status;

  if (s === 'DRAFT') {
    actions.push({ label: '提交审查', target: 'UNDER_REVIEW', tone: 'success' });
  }

  if (s === 'UNDER_REVIEW' && contract.latestReview?.reviewDecision === 'APPROVED') {
    actions.push({ label: '标记已签署', target: 'SIGNED', tone: 'success' });
  }

  if (s === 'SIGNED') {
    actions.push({ label: '开始履约', target: 'IN_PROGRESS', tone: 'success' });
  }

  if (s === 'IN_PROGRESS') {
    actions.push({ label: '标记完成', target: 'COMPLETED', tone: 'success' });
  }

  if (s !== 'COMPLETED' && s !== 'TERMINATED') {
    actions.push({ label: '终止', target: 'TERMINATED', tone: 'danger' });
  }

  return actions;
}

/** 金额格式化（人民币，最多两位小数）。 */
export function formatContractAmount(amount: number): string {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(amount);
}

/** ISO 时间格式化为本地时间；空值返回占位符，非法时间原样返回。 */
export function formatDateTime(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

export type { ContractReviewDecision };
