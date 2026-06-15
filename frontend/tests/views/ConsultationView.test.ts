import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import ConsultationView from '@/modules/consultation/views/ConsultationView.vue';
import { submitConsultation } from '@/shared/api/legal';

vi.mock('@/shared/api/legal', () => ({
  submitConsultation: vi.fn()
}));

const submitMock = vi.mocked(submitConsultation);

const stubs = {
  AiThinkingPanel: true,
  AiTracePanel: true,
  ConfidenceBadge: true,
  CitedText: true,
  RagSourceList: true
};

function mountView() {
  return mount(ConsultationView, { global: { stubs } });
}

function submitButton(wrapper: ReturnType<typeof mountView>) {
  return wrapper.findAll('button').find((b) => b.text().includes('开始分析'))!;
}

function questionField(wrapper: ReturnType<typeof mountView>) {
  return wrapper.find('textarea[placeholder^="例如：公司未签劳动合同"]');
}

beforeEach(() => {
  submitMock.mockReset();
  submitMock.mockResolvedValue({
    category: '劳动争议',
    legalBasis: [],
    recommendations: [],
    riskAlerts: [],
    confidence: 0.9,
    retrievalContext: {},
    answer: '答复内容'
  } as never);
});

afterEach(() => {
  document.body.innerHTML = '';
});

describe('ConsultationView', () => {
  it('问题为空时提交按钮禁用', () => {
    const wrapper = mountView();
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined();
  });

  it('填写问题后提交按钮可用', async () => {
    const wrapper = mountView();
    await questionField(wrapper).setValue('公司拖欠工资怎么办？');
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined();
  });

  it('空问题不会触发后端调用', async () => {
    const wrapper = mountView();
    // 直接触发点击（绕过 disabled 仍应被守卫拦截）
    await submitButton(wrapper).trigger('click');
    expect(submitMock).not.toHaveBeenCalled();
  });

  it('填写问题后提交会调用 submitConsultation', async () => {
    const wrapper = mountView();
    await questionField(wrapper).setValue('公司拖欠工资怎么办？');
    await submitButton(wrapper).trigger('click');
    expect(submitMock).toHaveBeenCalledTimes(1);
    expect(submitMock.mock.calls[0][0]).toMatchObject({
      question: '公司拖欠工资怎么办？'
    });
  });
});
