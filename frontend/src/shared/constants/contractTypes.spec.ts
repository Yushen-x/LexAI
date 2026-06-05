import { describe, expect, it } from 'vitest';
import { CONTRACT_TYPE_OPTIONS, CONTRACT_TYPE_VALUES } from './contractTypes';

describe('合同类型常量', () => {
  it('选项非空且 value/label 均有值', () => {
    expect(CONTRACT_TYPE_OPTIONS.length).toBeGreaterThan(0);
    for (const opt of CONTRACT_TYPE_OPTIONS) {
      expect(opt.value).toBeTruthy();
      expect(opt.label).toBeTruthy();
    }
  });

  it('value 唯一，避免下拉选项重复', () => {
    const values = CONTRACT_TYPE_OPTIONS.map((o) => o.value);
    expect(new Set(values).size).toBe(values.length);
  });

  it('CONTRACT_TYPE_VALUES 与选项 value 顺序一致', () => {
    expect(CONTRACT_TYPE_VALUES).toEqual(CONTRACT_TYPE_OPTIONS.map((o) => o.value));
  });
});
