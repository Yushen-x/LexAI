import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import ContractDraftView from '@/modules/contract/views/ContractDraftView.vue';
import { submitContractDraft } from '@/shared/api/legal';
import { createContract } from '@/shared/api/contracts';

const push = vi.fn();
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push })
}));

vi.mock('@/shared/api/legal', () => ({
  submitConsultation: vi.fn(),
  submitContractDraft: vi.fn()
}));

vi.mock('@/shared/api/contracts', () => ({
  createContract: vi.fn(),
  getContract: vi.fn(),
  updateContract: vi.fn()
}));

const draftMock = vi.mocked(submitContractDraft);
const createMock = vi.mocked(createContract);

function findButton(wrapper: any, label: string) {
  return wrapper.findAll('button').find((b: any) => b.text().includes(label));
}

beforeEach(() => {
  push.mockReset();
  draftMock.mockReset();
  createMock.mockReset();
  createMock.mockResolvedValue({ id: 1, contractNo: 'HT-001' } as never);
  draftMock.mockResolvedValue({
    title: '合同',
    generatedContent: '这是生成的合同正文',
    summary: '摘要',
    generatedAt: new Date().toISOString()
  } as never);
});

afterEach(() => {
  document.body.innerHTML = '';
});

describe('ContractDraftView', () => {
  it('合同名称为空时存草稿会被校验拦截', async () => {
    const wrapper = mount(ContractDraftView);
    await wrapper.find('.title-input').setValue('');
    await findButton(wrapper, '存草稿')!.trigger('click');
    await flushPromises();
    expect(createMock).not.toHaveBeenCalled();
  });

  it('默认有效表单存草稿会调用 createContract', async () => {
    const wrapper = mount(ContractDraftView);
    await findButton(wrapper, '存草稿')!.trigger('click');
    await flushPromises();
    expect(createMock).toHaveBeenCalledTimes(1);
    expect(createMock.mock.calls[0][0]).toMatchObject({ status: 'DRAFT' });
  });

  it('填齐信息后生成合同会调用 submitContractDraft 并回填正文', async () => {
    const wrapper = mount(ContractDraftView);
    await wrapper.find('input[placeholder="输入合作方名称..."]').setValue('某科技公司');
    await findButton(wrapper, '生成合同')!.trigger('click');
    await flushPromises();
    expect(draftMock).toHaveBeenCalledTimes(1);
    expect((wrapper.find('textarea.doc-textarea').element as HTMLTextAreaElement).value).toContain(
      '这是生成的合同正文'
    );
  });

  it('正文为空时「提交审查」按钮禁用，填入后可用', async () => {
    const wrapper = mount(ContractDraftView);
    const submitBtn = findButton(wrapper, '提交审查')!;
    expect(submitBtn.attributes('disabled')).toBeDefined();

    await wrapper.find('textarea.doc-textarea').setValue('合同正文内容');
    expect(findButton(wrapper, '提交审查')!.attributes('disabled')).toBeUndefined();
  });
});
