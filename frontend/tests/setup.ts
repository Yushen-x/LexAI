// Vitest 全局测试初始化。
// jsdom 默认不实现 document.execCommand，这里提供一个可被测试断言的桩，
// 供 clipboard 降级路径与组件复制行为使用。
import { vi } from 'vitest';

if (typeof document !== 'undefined' && typeof document.execCommand !== 'function') {
  // @ts-expect-error jsdom 未实现 execCommand，测试中以桩替代
  document.execCommand = vi.fn(() => true);
}
