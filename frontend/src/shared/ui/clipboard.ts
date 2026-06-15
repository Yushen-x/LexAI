/**
 * 复制文本到剪贴板。
 *
 * 线上以明文 HTTP 部署时，`navigator.clipboard` 在非安全上下文中不可用
 * （为 undefined 或调用即被拒绝），会导致所有“复制”按钮静默失效。
 * 这里统一优先走异步 Clipboard API，失败/不可用时降级到 `execCommand('copy')`，
 * 保证在 HTTP 与 HTTPS 下都能复制。
 *
 * @returns 复制是否成功，调用方据此提示用户。
 */
export async function copyText(text: string): Promise<boolean> {
  if (!text) return false;

  // 安全上下文（HTTPS / localhost）下优先使用异步 Clipboard API。
  if (typeof navigator !== 'undefined' && navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // 继续走下面的降级方案。
    }
  }

  return legacyCopy(text);
}

/** 基于临时 textarea + execCommand 的降级复制，兼容非安全上下文。 */
function legacyCopy(text: string): boolean {
  if (typeof document === 'undefined') return false;

  const textarea = document.createElement('textarea');
  textarea.value = text;
  // 移出视口并避免触发滚动/缩放。
  textarea.style.position = 'fixed';
  textarea.style.top = '-9999px';
  textarea.style.left = '-9999px';
  textarea.setAttribute('readonly', '');
  document.body.appendChild(textarea);

  let ok = false;
  try {
    textarea.select();
    textarea.setSelectionRange(0, text.length);
    ok = document.execCommand('copy');
  } catch {
    ok = false;
  } finally {
    document.body.removeChild(textarea);
  }
  return ok;
}
