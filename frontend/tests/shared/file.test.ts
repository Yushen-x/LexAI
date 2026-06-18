import { describe, expect, it } from 'vitest';
import { sanitizeFileName } from '@/shared/utils/file';

describe('sanitizeFileName', () => {
  it('正常名称保持不变', () => {
    expect(sanitizeFileName('云服务采购合同')).toBe('云服务采购合同');
  });

  it('替换路径分隔符与非法字符为下划线', () => {
    expect(sanitizeFileName('a/b\\c:d*e?f"g<h>i|j')).toBe('a_b_c_d_e_f_g_h_i_j');
  });

  it('去掉开头的点，避免隐藏文件', () => {
    expect(sanitizeFileName('...隐藏')).toBe('隐藏');
  });

  it('空白折叠并去除首尾空格', () => {
    expect(sanitizeFileName('  合同   名称  ')).toBe('合同 名称');
  });

  it('空名称回退到默认值', () => {
    expect(sanitizeFileName('')).toBe('合同');
    expect(sanitizeFileName('///')).toBe('___'); // 仅非法字符仍保留为下划线
  });

  it('可自定义回退名', () => {
    expect(sanitizeFileName('', '草稿')).toBe('草稿');
  });

  it('限制长度不超过 100', () => {
    expect(sanitizeFileName('字'.repeat(200)).length).toBe(100);
  });
});
