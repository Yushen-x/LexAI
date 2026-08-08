import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import ContractReviewView from '@/modules/contract-review/views/ContractReviewView.vue';
import { submitContractReview } from '@/shared/api/legal';

const push = vi.fn();
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push })
}));

vi.mock('@/shared/api/legal', () => ({
  submitContractReview: vi.fn()
}));

vi.mock('@/shared/api/contracts', () => ({
  getContract: vi.fn(),
  updateContract: vi.fn(),
  updateContractReview: vi.fn()
}));

const reviewMock = vi.mocked(submitContractReview);

const stubs = {
  AiThinkingPanel: true,
  AiTracePanel: true,
  ConfidenceBadge: true,
  CitedText: true,
  RagSourceList: true
};

function mountView() {
  return mount(ContractReviewView, { global: { stubs } });
}

function findButton(wrapper: any, label: string) {
  return wrapper.findAll('button').find((b: any) => b.text().includes(label));
}

beforeEach(() => {
  push.mockReset();
  reviewMock.mockReset();
  reviewMock.mockResolvedValue({
    summary: '审查摘要',
    risks: [],
    missingClauses: [],
    suggestions: [],
    reviewDecision: 'PENDING_CONFIRMATION',
    confidence: 0.8,
    retrievalContext: {}
  } as never);
});

afterEach(() => {
  document.body.innerHTML = '';
  sessionStorage.clear();
});

describe('ContractReviewView', () => {
  it('试用模式下正文为空时提交被拦截，不调用接口', async () => {
    const wrapper = mountView();
    await flushPromises();
    await findButton(wrapper, '开始智能审查')!.trigger('click');
    await flushPromises();
    expect(reviewMock).not.toHaveBeenCalled();
  });

  it('点击预设填充正文后可提交并调用 submitContractReview', async () => {
    const wrapper = mountView();
    await flushPromises();

    // 点击第一个预设填充标题与正文
    const preset = wrapper.findAll('.presets-row button')[0];
    await preset.trigger('click');
    await flushPromises();

    const content = (wrapper.find('#contractContent').element as HTMLTextAreaElement).value;
    expect(content.length).toBeGreaterThan(0);

    await findButton(wrapper, '开始智能审查')!.trigger('click');
    await flushPromises();
    expect(reviewMock).toHaveBeenCalledTimes(1);
  });

  it('手动输入正文也能提交审查', async () => {
    const wrapper = mountView();
    await flushPromises();
    await wrapper.find('#contractContent').setValue('甲方与乙方就服务事项达成如下协议……');
    await findButton(wrapper, '开始智能审查')!.trigger('click');
    await flushPromises();
    expect(reviewMock).toHaveBeenCalledTimes(1);
    expect(reviewMock.mock.calls[0][0]).toMatchObject({
      contractContent: expect.stringContaining('甲方与乙方')
    });
  });
});
