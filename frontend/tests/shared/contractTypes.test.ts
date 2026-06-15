import { describe, expect, it } from 'vitest';
import {
  CONTRACT_TYPE_OPTIONS,
  CONTRACT_TYPE_VALUES
} from '@/shared/constants/contractTypes';

describe('contractTypes 常量', () => {
  it('选项非空且 value/label 齐全', () => {
    expect(CONTRACT_TYPE_OPTIONS.length).toBeGreaterThan(0);
    for (const opt of CONTRACT_TYPE_OPTIONS) {
      expect(opt.value).toBeTruthy();
      expect(opt.label).toBeTruthy();
    }
  });

  it('VALUES 与 OPTIONS 的 value 一一对应', () => {
    expect(CONTRACT_TYPE_VALUES).toEqual(CONTRACT_TYPE_OPTIONS.map((o) => o.value));
  });

  it('value 无重复', () => {
    expect(new Set(CONTRACT_TYPE_VALUES).size).toBe(CONTRACT_TYPE_VALUES.length);
  });
});
