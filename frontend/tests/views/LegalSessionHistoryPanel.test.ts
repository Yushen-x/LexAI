import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import LegalSessionHistoryPanel from '@/shared/ui/LegalSessionHistoryPanel.vue';
import { fetchLegalSessionDetail, fetchLegalSessions } from '@/shared/api/legal';

vi.mock('@/shared/api/legal', () => ({
  fetchLegalSessions: vi.fn(),
  fetchLegalSessionDetail: vi.fn()
}));

const listMock = vi.mocked(fetchLegalSessions);
const detailMock = vi.mocked(fetchLegalSessionDetail);

function sessionPage(content: unknown[], totalElements = content.length) {
  return { content, totalElements, page: 0, size: 10 };
}

function mountPanel() {
  return mount(LegalSessionHistoryPanel, {
    props: { scenarioType: 'CONSULTATION' as never }
  });
}

beforeEach(() => {
  listMock.mockReset();
  detailMock.mockReset();
});

afterEach(() => {
  document.body.innerHTML = '';
});

describe('LegalSessionHistoryPanel', () => {
  it('无历史时显示空态', async () => {
    listMock.mockResolvedValue(sessionPage([]) as never);
    const wrapper = mountPanel();
    await flushPromises();
    expect(wrapper.text()).toContain('暂无历史记录');
  });

  it('渲染历史条目标题', async () => {
    listMock.mockResolvedValue(
      sessionPage([
        { id: 1, title: '劳动争议咨询', createdAt: new Date().toISOString(), confidence: 0.9 }
      ]) as never
    );
    const wrapper = mountPanel();
    await flushPromises();
    expect(wrapper.text()).toContain('劳动争议咨询');
    expect(wrapper.text()).toContain('90%');
  });

  it('点击条目加载详情并 emit restore', async () => {
    listMock.mockResolvedValue(
      sessionPage([{ id: 7, title: '会话', createdAt: new Date().toISOString() }]) as never
    );
    detailMock.mockResolvedValue({
      inputPayload: '{"question":"x"}',
      outputPayload: '{"answer":"y"}'
    } as never);

    const wrapper = mountPanel();
    await flushPromises();
    await wrapper.find('.history-btn').trigger('click');
    await flushPromises();

    expect(detailMock).toHaveBeenCalledWith(7);
    expect(wrapper.emitted('restore')?.[0][0]).toMatchObject({
      inputPayload: '{"question":"x"}',
      outputPayload: '{"answer":"y"}'
    });
  });

  it('搜索会按关键词重新请求', async () => {
    listMock.mockResolvedValue(sessionPage([]) as never);
    const wrapper = mountPanel();
    await flushPromises();

    await wrapper.find('.search-input').setValue('工资');
    await wrapper.find('.search-btn').trigger('click');
    await flushPromises();

    const lastCall = listMock.mock.calls.at(-1)!;
    expect(lastCall[0]).toBe('CONSULTATION');
    expect(lastCall[3]).toBe('工资');
  });

  it('还有更多数据时显示「加载更多」', async () => {
    listMock.mockResolvedValue(
      sessionPage(
        [{ id: 1, title: 'A', createdAt: new Date().toISOString() }],
        5
      ) as never
    );
    const wrapper = mountPanel();
    await flushPromises();
    expect(wrapper.text()).toContain('加载更多');
  });
});
