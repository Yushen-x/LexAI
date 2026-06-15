import { describe, expect, it } from 'vitest';
import {
  formatContractAmount,
  formatDateTime,
  isStageBefore,
  nextStatusActions,
  reviewDecisionLabel,
  statusLabel,
  statusTone
} from '@/shared/contracts/lifecycle';
import type { ContractItem, ContractReviewDecision, ContractStatus } from '@/shared/types/contracts';

function makeContract(
  status: ContractStatus,
  reviewDecision?: ContractReviewDecision
): ContractItem {
  return {
    id: 1,
    contractNo: 'HT-001',
    name: '测试合同',
    contractType: '采购合同',
    partyA: '甲方',
    partyB: '乙方',
    amount: 1000,
    content: '',
    status,
    source: null,
    latestReview: reviewDecision
      ? {
          summary: '',
          risks: [],
          missingClauses: [],
          reviewerOpinion: '',
          reviewDecision,
          reviewedAt: null
        }
      : null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };
}

describe('statusLabel', () => {
  it('返回已知状态的中文标签', () => {
    expect(statusLabel('DRAFT')).toBe('草稿');
    expect(statusLabel('TERMINATED')).toBe('已终止');
  });

  it('未知状态原样返回', () => {
    expect(statusLabel('SOMETHING_NEW')).toBe('SOMETHING_NEW');
  });
});

describe('statusTone', () => {
  it.each<[ContractStatus, string]>([
    ['DRAFT', 'muted'],
    ['UNDER_REVIEW', 'warning'],
    ['SIGNED', 'success'],
    ['IN_PROGRESS', 'success'],
    ['COMPLETED', 'success'],
    ['TERMINATED', 'danger']
  ])('%s -> %s', (status, tone) => {
    expect(statusTone(status)).toBe(tone);
  });
});

describe('reviewDecisionLabel', () => {
  it('映射已知决策', () => {
    expect(reviewDecisionLabel('APPROVED')).toBe('审查通过');
    expect(reviewDecisionLabel('NEEDS_REVISION')).toBe('退回修改');
    expect(reviewDecisionLabel('PENDING_CONFIRMATION')).toBe('待人工确认');
  });

  it('未知决策回退到待人工确认', () => {
    expect(reviewDecisionLabel('???')).toBe('待人工确认');
  });
});

describe('isStageBefore', () => {
  it('靠前的阶段判定为 true', () => {
    expect(isStageBefore('DRAFT', 'SIGNED')).toBe(true);
  });

  it('同阶段或靠后判定为 false', () => {
    expect(isStageBefore('SIGNED', 'SIGNED')).toBe(false);
    expect(isStageBefore('COMPLETED', 'DRAFT')).toBe(false);
  });

  it('不在主线（如 TERMINATED）返回 false', () => {
    expect(isStageBefore('TERMINATED', 'SIGNED')).toBe(false);
  });
});

describe('nextStatusActions', () => {
  it('DRAFT 可提交审查并可终止', () => {
    const actions = nextStatusActions(makeContract('DRAFT'));
    expect(actions.map((a) => a.target)).toEqual(['UNDER_REVIEW', 'TERMINATED']);
  });

  it('UNDER_REVIEW 仅在 AI 审查通过后才能标记已签署', () => {
    const pending = nextStatusActions(makeContract('UNDER_REVIEW', 'PENDING_CONFIRMATION'));
    expect(pending.map((a) => a.target)).toEqual(['TERMINATED']);

    const approved = nextStatusActions(makeContract('UNDER_REVIEW', 'APPROVED'));
    expect(approved.map((a) => a.target)).toEqual(['SIGNED', 'TERMINATED']);
  });

  it('SIGNED -> 开始履约；IN_PROGRESS -> 标记完成', () => {
    expect(nextStatusActions(makeContract('SIGNED')).map((a) => a.target)).toEqual([
      'IN_PROGRESS',
      'TERMINATED'
    ]);
    expect(nextStatusActions(makeContract('IN_PROGRESS')).map((a) => a.target)).toEqual([
      'COMPLETED',
      'TERMINATED'
    ]);
  });

  it('COMPLETED 与 TERMINATED 为终态，无可用操作', () => {
    expect(nextStatusActions(makeContract('COMPLETED'))).toEqual([]);
    expect(nextStatusActions(makeContract('TERMINATED'))).toEqual([]);
  });

  it('终止操作标记为 danger 色调', () => {
    const terminate = nextStatusActions(makeContract('DRAFT')).find(
      (a) => a.target === 'TERMINATED'
    );
    expect(terminate?.tone).toBe('danger');
  });
});

describe('formatContractAmount', () => {
  it('按千分位格式化金额', () => {
    expect(formatContractAmount(1234567)).toBe('1,234,567');
  });

  it('保留最多两位小数', () => {
    expect(formatContractAmount(1234.5)).toBe('1,234.5');
  });
});

describe('formatDateTime', () => {
  it('空字符串返回占位符', () => {
    expect(formatDateTime('')).toBe('—');
  });

  it('非法时间原样返回', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date');
  });

  it('合法 ISO 时间被格式化（包含年份）', () => {
    expect(formatDateTime('2026-06-15T08:30:00Z')).toContain('2026');
  });
});
