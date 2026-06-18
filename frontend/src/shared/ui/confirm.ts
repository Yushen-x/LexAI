// 轻量的 Promise 化确认弹窗，风格与 toast.ts 一致：命令式挂载到 body，
// 无需在 App.vue 注册组件。用于危险/不可逆操作（如合同终止、保存人工复核）的二次确认。

export interface ConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  /** 危险操作：确认按钮显示为红色 */
  danger?: boolean;
}

const STYLE_ID = 'lexai-confirm-style';

function ensureStyle(): void {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement('style');
  style.id = STYLE_ID;
  style.textContent = `
.lexai-confirm-mask {
  position: fixed; inset: 0; z-index: 3000;
  background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center;
  animation: lexai-confirm-fade 0.15s ease-out;
}
.lexai-confirm-dialog {
  background: var(--bg-surface, #fff); color: var(--text-main, #1f2937);
  border-radius: 12px; width: min(420px, calc(100vw - 2rem));
  box-shadow: 0 20px 50px rgba(0,0,0,0.25); overflow: hidden;
}
.lexai-confirm-body { padding: 1.25rem 1.5rem 1rem; }
.lexai-confirm-title { font-size: 1rem; font-weight: 600; margin: 0 0 0.5rem; color: var(--text-strong, #111827); }
.lexai-confirm-msg { font-size: 0.9rem; line-height: 1.6; margin: 0; color: var(--text-muted, #4b5563); }
.lexai-confirm-actions { display: flex; justify-content: flex-end; gap: 0.6rem; padding: 0.9rem 1.5rem 1.2rem; }
.lexai-confirm-btn {
  padding: 0.45rem 1.1rem; border-radius: 8px; font-size: 0.875rem; cursor: pointer;
  border: 1px solid var(--border-light, #e5e7eb); background: var(--bg-app, #f8fafc); color: var(--text-main, #374151);
  transition: all 0.15s;
}
.lexai-confirm-btn:hover { border-color: #cbd5e1; }
.lexai-confirm-btn--primary { background: var(--primary, #2563eb); border-color: var(--primary, #2563eb); color: #fff; }
.lexai-confirm-btn--primary:hover { filter: brightness(0.95); }
.lexai-confirm-btn--danger { background: #dc2626; border-color: #dc2626; color: #fff; }
.lexai-confirm-btn--danger:hover { filter: brightness(0.95); }
@keyframes lexai-confirm-fade { from { opacity: 0; } to { opacity: 1; } }
`;
  document.head.appendChild(style);
}

/**
 * 弹出确认框，返回用户是否确认。
 * 取消 / 点遮罩 / Esc 均视为否（resolve false）。
 */
export function confirmAction(options: ConfirmOptions): Promise<boolean> {
  ensureStyle();

  return new Promise<boolean>((resolve) => {
    const mask = document.createElement('div');
    mask.className = 'lexai-confirm-mask';
    mask.setAttribute('role', 'dialog');
    mask.setAttribute('aria-modal', 'true');

    const dialog = document.createElement('div');
    dialog.className = 'lexai-confirm-dialog';

    const body = document.createElement('div');
    body.className = 'lexai-confirm-body';
    if (options.title) {
      const title = document.createElement('p');
      title.className = 'lexai-confirm-title';
      title.textContent = options.title;
      body.appendChild(title);
    }
    const msg = document.createElement('p');
    msg.className = 'lexai-confirm-msg';
    msg.textContent = options.message;
    body.appendChild(msg);

    const actions = document.createElement('div');
    actions.className = 'lexai-confirm-actions';

    const cancelBtn = document.createElement('button');
    cancelBtn.type = 'button';
    cancelBtn.className = 'lexai-confirm-btn';
    cancelBtn.textContent = options.cancelText ?? '取消';

    const confirmBtn = document.createElement('button');
    confirmBtn.type = 'button';
    confirmBtn.className =
      'lexai-confirm-btn ' + (options.danger ? 'lexai-confirm-btn--danger' : 'lexai-confirm-btn--primary');
    confirmBtn.textContent = options.confirmText ?? '确定';

    actions.appendChild(cancelBtn);
    actions.appendChild(confirmBtn);
    dialog.appendChild(body);
    dialog.appendChild(actions);
    mask.appendChild(dialog);
    document.body.appendChild(mask);

    let settled = false;
    function cleanup(result: boolean): void {
      if (settled) return;
      settled = true;
      document.removeEventListener('keydown', onKey);
      mask.remove();
      resolve(result);
    }
    function onKey(e: KeyboardEvent): void {
      if (e.key === 'Escape') cleanup(false);
      else if (e.key === 'Enter') cleanup(true);
    }

    cancelBtn.addEventListener('click', () => cleanup(false));
    confirmBtn.addEventListener('click', () => cleanup(true));
    mask.addEventListener('click', (e) => {
      if (e.target === mask) cleanup(false);
    });
    document.addEventListener('keydown', onKey);
    confirmBtn.focus();
  });
}
