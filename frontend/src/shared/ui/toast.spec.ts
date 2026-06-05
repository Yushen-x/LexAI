import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from './toast';

const CONTAINER_ID = 'lexai-toast-container';

beforeEach(() => {
  vi.useFakeTimers();
  document.body.innerHTML = '';
});

afterEach(() => {
  vi.useRealTimers();
});

describe('toast', () => {
  it('空消息直接忽略，不创建容器', () => {
    toast('');
    expect(document.getElementById(CONTAINER_ID)).toBeNull();
  });

  it('展示消息时挂载容器与对应类型的提示元素', () => {
    toast('保存成功', 'success');
    const container = document.getElementById(CONTAINER_ID);
    expect(container).not.toBeNull();
    const el = container!.querySelector('.lexai-toast');
    expect(el).not.toBeNull();
    expect(el!.textContent).toBe('保存成功');
    expect(el!.classList.contains('lexai-toast--success')).toBe(true);
    expect(el!.getAttribute('role')).toBe('status');
  });

  it('多条提示复用同一个容器', () => {
    toast('a');
    toast('b');
    const containers = document.querySelectorAll(`#${CONTAINER_ID}`);
    expect(containers.length).toBe(1);
    expect(containers[0].querySelectorAll('.lexai-toast').length).toBe(2);
  });

  it('超时后自动移除，容器清空后一并移除', () => {
    toast('稍后消失', 'info', 1000);
    expect(document.querySelectorAll('.lexai-toast').length).toBe(1);

    // 先走完展示时长，再走完淡出动画的 200ms。
    vi.advanceTimersByTime(1000);
    vi.advanceTimersByTime(200);

    expect(document.querySelectorAll('.lexai-toast').length).toBe(0);
    expect(document.getElementById(CONTAINER_ID)).toBeNull();
  });

  it('点击提示立即移除', () => {
    toast('点我关闭');
    const el = document.querySelector('.lexai-toast') as HTMLElement;
    el.click();
    expect(document.querySelectorAll('.lexai-toast').length).toBe(0);
    expect(document.getElementById(CONTAINER_ID)).toBeNull();
  });
});
