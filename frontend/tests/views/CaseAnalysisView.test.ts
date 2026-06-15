import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import CaseAnalysisView from '@/modules/case-analysis/views/CaseAnalysisView.vue';
import { submitCaseAnalysis } from '@/shared/api/legal';

vi.mock('@/shared/api/legal', () => ({
  submitCaseAnalysis: vi.fn()
}));

const submitMock = vi.mocked(submitCaseAnalysis);

const stubs = {
  AiThinkingPanel: true,
  AiTracePanel: true,
  ConfidenceBadge: true,
  CitedText: true,
  RagSourceList: true
};

function mountView() {
  return mount(CaseAnalysisView, { global: { stubs } });
}

function submitButton(wrapper: ReturnType<typeof mountView>) {
  return wrapper.findAll('button').find((b) => b.text().includes('生成案件画像'))!;
}

function summaryField(wrapper: ReturnType<typeof mountView>) {
  return wrapper.find('textarea[placeholder^="请描述案件背景"]');
}

beforeEach(() => {
  submitMock.mockReset();
  submitMock.mockResolvedValue({
    keyFacts: [],
    disputedIssues: [],
    evidenceGaps: [],
    suggestedActions: [],
    confidence: 0.8,
    retrievalContext: {}
  } as never);
});

afterEach(() => {
  document.body.innerHTML = '';
});

describe('CaseAnalysisView', () => {
  it('案情为空时提交按钮禁用', () => {
    const wrapper = mountView();
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined();
  });

  it('填写案情后按钮可用', async () => {
    const wrapper = mountView();
    await summaryField(wrapper).setValue('甲乙双方因合同履约产生争议');
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined();
  });

  it('空案情不会触发后端调用', async () => {
    const wrapper = mountView();
    await submitButton(wrapper).trigger('click');
    expect(submitMock).not.toHaveBeenCalled();
  });

  it('填写案情后提交会调用 submitCaseAnalysis', async () => {
    const wrapper = mountView();
    await summaryField(wrapper).setValue('甲乙双方因合同履约产生争议');
    await submitButton(wrapper).trigger('click');
    expect(submitMock).toHaveBeenCalledTimes(1);
    expect(submitMock.mock.calls[0][0]).toMatchObject({
      caseSummary: '甲乙双方因合同履约产生争议'
    });
  });
});
