import { afterEach, describe, expect, it, vi } from 'vitest';
import { copyText } from '@/shared/ui/clipboard';

function setSecureContext(value: boolean): void {
  Object.defineProperty(window, 'isSecureContext', {
    value,
    configurable: true
  });
}

function setClipboard(writeText: ((t: string) => Promise<void>) | undefined): void {
  Object.defineProperty(navigator, 'clipboard', {
    value: writeText ? { writeText } : undefined,
    configurable: true
  });
}

afterEach(() => {
  vi.restoreAllMocks();
  setClipboard(undefined);
});

describe('copyText', () => {
  it('空文本直接返回 false', async () => {
    expect(await copyText('')).toBe(false);
  });

  it('安全上下文下优先使用 navigator.clipboard', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    setSecureContext(true);
    setClipboard(writeText);

    expect(await copyText('hello')).toBe(true);
    expect(writeText).toHaveBeenCalledWith('hello');
  });

  it('非安全上下文（明文 HTTP）降级到 execCommand', async () => {
    setSecureContext(false);
    setClipboard(undefined);
    const exec = vi.spyOn(document, 'execCommand').mockReturnValue(true);

    expect(await copyText('via-fallback')).toBe(true);
    expect(exec).toHaveBeenCalledWith('copy');
  });

  it('clipboard 抛错时回退到 execCommand', async () => {
    setSecureContext(true);
    setClipboard(vi.fn().mockRejectedValue(new Error('denied')));
    const exec = vi.spyOn(document, 'execCommand').mockReturnValue(true);

    expect(await copyText('boom')).toBe(true);
    expect(exec).toHaveBeenCalledWith('copy');
  });

  it('execCommand 失败时返回 false', async () => {
    setSecureContext(false);
    setClipboard(undefined);
    vi.spyOn(document, 'execCommand').mockReturnValue(false);

    expect(await copyText('nope')).toBe(false);
  });
});
