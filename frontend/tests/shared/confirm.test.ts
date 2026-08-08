import { afterEach, describe, expect, it } from 'vitest';
import { confirmAction } from '@/shared/ui/confirm';

afterEach(() => {
  document.body.innerHTML = '';
  document.head.querySelector('#lexai-confirm-style')?.remove();
});

function dialog() {
  return document.querySelector('.lexai-confirm-dialog');
}

describe('confirmAction', () => {
  it('渲染标题、消息与按钮文案', () => {
    confirmAction({ title: '终止合同', message: '确定吗？', confirmText: '确认终止' });
    expect(document.querySelector('.lexai-confirm-title')?.textContent).toBe('终止合同');
    expect(document.querySelector('.lexai-confirm-msg')?.textContent).toBe('确定吗？');
    const buttons = document.querySelectorAll('.lexai-confirm-btn');
    expect(buttons[0].textContent).toBe('取消');
    expect(buttons[1].textContent).toBe('确认终止');
  });

  it('点确认 resolve true 并移除弹窗', async () => {
    const p = confirmAction({ message: 'x' });
    (document.querySelectorAll('.lexai-confirm-btn')[1] as HTMLButtonElement).click();
    await expect(p).resolves.toBe(true);
    expect(dialog()).toBeNull();
  });

  it('点取消 resolve false', async () => {
    const p = confirmAction({ message: 'x' });
    (document.querySelectorAll('.lexai-confirm-btn')[0] as HTMLButtonElement).click();
    await expect(p).resolves.toBe(false);
  });

  it('按 Esc resolve false', async () => {
    const p = confirmAction({ message: 'x' });
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await expect(p).resolves.toBe(false);
  });

  it('danger 选项让确认按钮带危险样式', () => {
    confirmAction({ message: 'x', danger: true });
    expect(document.querySelector('.lexai-confirm-btn--danger')).not.toBeNull();
  });
});
